/*
 * Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 * modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 * PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 * OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 * YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 * OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 * COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 * BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 * OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 * PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 * this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */
package de.ukbonn.mwtek.dashboard.services;

import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_CONDITIONS;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_OBSERVATIONS_PCR;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_OBSERVATIONS_VARIANTS;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_PROCEDURES_ECMO;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_PROCEDURES_VENTILATION;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.INFLUENZA_CONDITIONS;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.INFLUENZA_OBSERVATIONS_PCR;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.KIDS_RADAR_CPAP;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.KIDS_RADAR_HIGH_FLOW;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.KIDS_RADAR_OPS_BASE;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.extractInputCodes;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.extractKidsRadarDiagnosisConditions;
import static de.ukbonn.mwtek.dashboard.misc.FhirServerQuerySuffixBuilder.getIcuProcedureCodesAsList;
import static de.ukbonn.mwtek.dashboard.misc.IcuStayDetection.addIcuDummyLocationDueOtherCriteria;
import static de.ukbonn.mwtek.dashboard.misc.KiRaChecks.isConditionNeeded;
import static de.ukbonn.mwtek.dashboard.misc.KiRaChecks.isProcedureNeededForKiRa;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.encounterIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.locationIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.patientIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboard.misc.ResourceHandler.isEncounterInpatientFacilityContact;
import static de.ukbonn.mwtek.dashboard.misc.ResourceHandler.isProcedureStatusValid;
import static de.ukbonn.mwtek.dashboard.misc.ResourceHandler.linkConditionsToEncounters;
import static de.ukbonn.mwtek.dashboard.misc.ResourceHandler.removeNotNeededAttributes;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.ACRIBIS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.BCT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.KJP;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.PED;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.extractIdFromReference;
import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getKickOffDateStringFormat;
import static de.ukbonn.mwtek.utilities.generic.collections.ListTools.splitList;
import static de.ukbonn.mwtek.utilities.generic.time.DateTools.dateToFhirSearchSyntax;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import de.ukbonn.mwtek.dashboard.DashboardApplication;
import de.ukbonn.mwtek.dashboard.configuration.CustomGlobalConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.FhirSearchConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.FhirServerRestConfiguration;
import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import de.ukbonn.mwtek.dashboard.exceptions.SearchException;
import de.ukbonn.mwtek.dashboard.interfaces.DataSourceType;
import de.ukbonn.mwtek.dashboard.interfaces.SearchService;
import de.ukbonn.mwtek.dashboard.misc.AcribisChecks;
import de.ukbonn.mwtek.dashboard.misc.FhirServerQuerySuffixBuilder;
import de.ukbonn.mwtek.dashboard.misc.ResourceHandler;
import de.ukbonn.mwtek.dashboardlogic.enums.AcribisCohortOpsCodes;
import de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.KidsRadar;
import de.ukbonn.mwtek.dashboardlogic.models.PidTimestampCohortMap;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter;
import de.ukbonn.mwtek.utilities.fhir.interfaces.PatientIdentifierValueProvider;
import de.ukbonn.mwtek.utilities.fhir.misc.FhirConditionTools;
import de.ukbonn.mwtek.utilities.fhir.misc.ResourceConverter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiConsent;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiQuestionnaireResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;

/**
 * All methods to retrieve the data necessary for the corona dashboard from any FHIR server.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class FhirDataRetrievalService extends AbstractDataRetrievalService {

  public static final String NEXT = "next";
  private final FhirSearchConfiguration fhirSearchConfiguration;
  private final FhirServerQuerySuffixBuilder fhirServerQuerySuffixBuilder =
      new FhirServerQuerySuffixBuilder();
  private final FhirServerRestConfiguration fhirServerRestConfiguration;
  Logger logger = LoggerFactory.getLogger(DashboardApplication.class);

  /** List with the entries of the bundles returned per FHIR search query */
  List<BundleEntryComponent> reqBundleEntry = null;

  public FhirDataRetrievalService(
      SearchService searchService,
      FhirSearchConfiguration fhirSearchConfiguration,
      CustomGlobalConfiguration customGlobalConfiguration,
      FhirServerRestConfiguration fhirServerRestConfiguration) {
    this.fhirSearchConfiguration = fhirSearchConfiguration;
    this.fhirServerRestConfiguration = fhirServerRestConfiguration;
    initializeSearchService(searchService, fhirSearchConfiguration, customGlobalConfiguration);
  }

  /**
   * Initialize everything related to the server searches (syntax codes, user configurations).
   *
   * @param searchService The service that performs the server search requests
   * @param fhirSearchConfiguration The configuration of the {@link ServerTypeEnum#FHIR} server from
   *     application.yaml
   */
  private void initializeSearchService(
      SearchService searchService,
      FhirSearchConfiguration fhirSearchConfiguration,
      CustomGlobalConfiguration customGlobalConfiguration) {
    this.setSearchService(searchService);
    this.setSearchConfiguration(fhirSearchConfiguration);
    this.setCustomGlobalConfiguration(customGlobalConfiguration);
    // Read the Sars-Cov2 PCR codes from the configuration and persist them in a list analogous
    // to the Acuwave parameterization (FHIR servers expect a comma separated list of strings
    // while the Acuwave needs an integer list)
    this.setCovidLabPcrCodes(extractInputCodes(customGlobalConfiguration, COVID_OBSERVATIONS_PCR));
    this.setCovidLabVariantCodes(
        extractInputCodes(customGlobalConfiguration, COVID_OBSERVATIONS_VARIANTS));
    this.setProcedureHighFlowCodes(
        extractInputCodes(customGlobalConfiguration, KIDS_RADAR_HIGH_FLOW));
    this.setProcedureCpapCodes(extractInputCodes(customGlobalConfiguration, KIDS_RADAR_CPAP));
    this.setProcedureVentilationCodes(
        extractInputCodes(customGlobalConfiguration, COVID_PROCEDURES_VENTILATION));
    this.setProcedureEcmoCodes(extractInputCodes(customGlobalConfiguration, COVID_PROCEDURES_ECMO));
    // Reading of the icd codes from the configuration and transforming it into a list
    this.setCovidIcdCodes(extractInputCodes(customGlobalConfiguration, COVID_CONDITIONS));
    // Influenza data setter
    this.setInfluenzaIcdCodes(extractInputCodes(customGlobalConfiguration, INFLUENZA_CONDITIONS));
    this.setInfluenzaLabPcrCodes(
        extractInputCodes(customGlobalConfiguration, INFLUENZA_OBSERVATIONS_PCR));
    this.setKidsRadarKjpIcdCodes(
        extractKidsRadarDiagnosisConditions(customGlobalConfiguration, KJP));
    this.setKidsRadarRsvIcdCodes(
        extractKidsRadarDiagnosisConditions(customGlobalConfiguration, PED));
    this.setKjpOpsCodePrefix(
        extractInputCodes(customGlobalConfiguration, KIDS_RADAR_OPS_BASE).getFirst());
  }

  @Override
  public List<Observation> getObservations(DataItemContext dataItemContext) {

    HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();
    boolean useGet = httpMethod.equals(GET);

    Bundle initialBundle =
        this.getSearchService()
            .getInitialBundle(
                fhirServerQuerySuffixBuilder.getObservations(
                    this, null, false, dataItemContext, useGet),
                httpMethod,
                ResourceType.Observation.name());
    List<Observation> listObservations = new ArrayList<>();
    int resourcesTotal = initialBundle.getTotal();

    // Servers like the Blaze do not support the bundle.total, so we retrieve it with an
    // additional fhir search query
    if (!initialBundle.hasTotal()) {
      resourcesTotal =
          this.getSearchService()
              .getInitialBundle(
                  fhirServerQuerySuffixBuilder.getObservations(
                      this, null, true, dataItemContext, useGet),
                  httpMethod,
                  ResourceType.Observation.name())
              .getTotal();
    }

    int counterObs = 0;
    // FHIR servers normally deliver the data in bundles. Navigation is done via the link
    // attribute. "Self" contains the current query and "Next" the link to retrieve the following
    // bundle.
    while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
      // Parsing the retrieved resources and reading out the patients and Encounter Ids for later
      // data queries.
      ResourceHandler.handleObservationEntries(
          initialBundle, listObservations, patientIds, encounterIds, this.getServerType());
      initialBundle =
          this.getSearchService()
              .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), GET);

      logStatusDataRetrievalSequential(
          resourcesTotal, counterObs++, FHIRAllTypes.OBSERVATION.getDisplay());
    }
    // No next link given? -> just add the results to the lists and go on
    ResourceHandler.handleObservationEntries(
        initialBundle, listObservations, patientIds, encounterIds, this.getServerType());

    return listObservations;
  }

  @Override
  public List<MiiObservation> getObservations(
      Collection<MiiEncounter> encounters,
      DataItemContext dataItemContext,
      List<String> loincCodes,
      QualitativeLabCodesSettings qualitativeLabCodesSettings)
      throws RestClientException, OutOfMemoryError {

    Set<MiiObservation> observations = ConcurrentHashMap.newKeySet();
    Set<String> encounterCaseIds =
        encounters.stream().map(Resource::getId).collect(Collectors.toSet());
    AtomicInteger filteredObservations = new AtomicInteger(0);
    List<List<String>> encounterIdSubsets =
        splitList(new ArrayList<>(encounterCaseIds), this.getBatchSize());
    encounterIdSubsets.parallelStream()
        .forEach(
            subList -> {
              var initialBundle = new Bundle();
              try {
                HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();
                boolean useGet = httpMethod.equals(GET);
                initialBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getObservations(
                                this, dataItemContext, loincCodes, subList, useGet),
                            httpMethod,
                            ResourceType.Observation.name());

                processObservationBundle(
                    initialBundle,
                    observations,
                    filteredObservations,
                    dataItemContext,
                    qualitativeLabCodesSettings);

                while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
                  initialBundle =
                      this.getSearchService()
                          .getBundlePart(
                              getNextUrl(fhirServerRestConfiguration, initialBundle), httpMethod);
                  processObservationBundle(
                      initialBundle,
                      observations,
                      filteredObservations,
                      dataItemContext,
                      qualitativeLabCodesSettings);
                }
              } catch (Exception e) {
                logErrorRetrieval("Condition", e);
              }
            });

    log.debug(
        "{} observations resources got filtered because no positive value was found.",
        filteredObservations.get());
    return new ArrayList<>(observations);
  }

  @Override
  public List<Condition> getConditions(DataItemContext dataItemContext) {

    boolean isUseEncounterConditionReference =
        fhirSearchConfiguration.isUseEncounterConditionReference();

    HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();
    boolean useGet = httpMethod.equals(GET);

    // Since the condition.encounter reference is not mandatory, it's possible to link encounter
    // and condition via encounter.diagnosis.
    Bundle initialBundle =
        !isUseEncounterConditionReference
            ? this.getSearchService()
                .getInitialBundle(
                    fhirServerQuerySuffixBuilder.getConditions(
                        this, null, false, dataItemContext, useGet),
                    httpMethod,
                    ResourceType.Condition.name())
            : this.getSearchService()
                .getInitialBundle(
                    fhirServerQuerySuffixBuilder.getConditionsIncludingEncounter(
                        this, dataItemContext, true),
                    GET,
                    ResourceType.Condition.name());
    List<Condition> listConditions = new ArrayList<>();
    // This list is just being used if a parameter is set in the configuration.
    List<Encounter> listEncounters = new ArrayList<>();
    int counterCond = 0;
    int resourcesTotal = initialBundle.getTotal();

    // Servers like the Blaze do not support the bundle.total, so we retrieve it with an
    // additional fhir search query
    if (!initialBundle.hasTotal()) {
      resourcesTotal =
          this.getSearchService()
              .getInitialBundle(
                  fhirServerQuerySuffixBuilder.getConditions(
                      this, null, true, dataItemContext, useGet),
                  httpMethod,
                  ResourceType.Condition.name())
              .getTotal();
    }

    while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
      // The handling differs, whether the output is condition resources only or the encounter
      // data needs to be retrieved as well.
      // -> Then we need to gather all resources first and make encounter id processing afterwards.
      if (!isUseEncounterConditionReference) {
        // Parsing the retrieved resources and reading out the patients and Encounter Ids for later
        // data queries.
        ResourceHandler.handleConditionEntries(
            initialBundle, listConditions, patientIds, encounterIds, this.getServerType());
      } else {
        // Gather the condition and encounter resources in corresponding lists.
        ResourceHandler.storeConditionAndEncounterResources(
            initialBundle, listConditions, listEncounters, this.getServerType());
      }
      initialBundle =
          this.getSearchService()
              .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), httpMethod);

      logStatusDataRetrievalSequential(
          resourcesTotal, counterCond++, FHIRAllTypes.CONDITION.getDisplay());
    }
    // no next link existent? -> just add the results to the lists and go further on
    if (!isUseEncounterConditionReference) {
      ResourceHandler.handleConditionEntries(
          initialBundle, listConditions, patientIds, encounterIds, this.getServerType());
    } else {
      ResourceHandler.storeConditionAndEncounterResources(
          initialBundle, listConditions, listEncounters, this.getServerType());
      ResourceHandler.handleConditionEntriesWithEncounterRefSetting(
          listConditions, listEncounters, patientIds, encounterIds, this.getServerType());
    }
    return listConditions;
  }

  @Override
  public List<MiiCondition> getConditions(
      Collection<MiiEncounter> encounters, DataItemContext dataItemContext)
      throws RestClientException {

    if (encounters == null || encounters.isEmpty()) {
      return List.of();
    }

    boolean isUseEncounterConditionReference =
        fhirSearchConfiguration.isUseEncounterConditionReference();

    // Extract case ids
    Set<String> encounterCaseIds =
        encounters.stream()
            .map(Resource::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    if (encounterCaseIds.isEmpty()) {
      return List.of();
    }

    Set<MiiCondition> setConditions = ConcurrentHashMap.newKeySet();
    AtomicInteger filteredConditions = new AtomicInteger(0);

    List<List<String>> encounterIdSubsets =
        splitList(new ArrayList<>(encounterCaseIds), this.getBatchSize());

    encounterIdSubsets.parallelStream()
        .forEach(
            subList -> {
              try {
                Bundle firstPage =
                    fetchInitialConditionsBundle(subList, isUseEncounterConditionReference);
                forEachBundlePage(
                    firstPage,
                    page ->
                        processConditionBundle(
                            page, setConditions, filteredConditions, dataItemContext));

              } catch (Exception e) {
                logErrorRetrieval("Condition", e);
              }
            });

    // Add condition.encounter references by encounter.diagnosis references if activated
    if (isUseEncounterConditionReference) {
      linkConditionsToEncounters(setConditions, encounters);
    }

    log.debug(
        "{} condition resources got filtered because no needed icd code was found.",
        filteredConditions.get());

    return new ArrayList<>(setConditions);
  }

  private Bundle fetchInitialConditionsBundle(
      List<String> encounterIds, boolean useEncounterDiagnosis) {
    HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();

    if (httpMethod != GET && httpMethod != POST) {
      throw new IllegalStateException("Unsupported HTTP method for search: " + httpMethod);
    }

    boolean httpMethodGet = (httpMethod == GET);

    // Select endpoint depending on search strategy
    String endpointName =
        useEncounterDiagnosis ? ResourceType.Encounter.name() : ResourceType.Condition.name();

    String suffix =
        useEncounterDiagnosis
            ? fhirServerQuerySuffixBuilder.getConditionsViaEncounterReference(
                this, encounterIds, httpMethodGet)
            : fhirServerQuerySuffixBuilder.getConditions(this, encounterIds, httpMethodGet);

    HttpMethod methodToUse = httpMethodGet ? GET : POST;

    // For the Encounter-based variant we still only consume Condition resources out of the bundle.
    return getSearchService().getInitialBundle(suffix, methodToUse, endpointName);
  }

  /** Iterates over all bundle pages and applies the consumer to each page. */
  private void forEachBundlePage(Bundle firstPage, Consumer<Bundle> pageConsumer) {
    Bundle current = firstPage;
    while (current != null) {
      pageConsumer.accept(current);
      if (current.hasLink() && current.getLink(NEXT) != null) {
        String nextUrl = getNextUrl(fhirServerRestConfiguration, current);
        current = this.getSearchService().getBundlePart(nextUrl, GET);
      } else {
        current = null;
      }
    }
  }

  @Override
  public List<MiiPatient> getPatients(
      List<MiiObservation> ukbObservations,
      List<MiiCondition> ukbConditions,
      DataItemContext dataItemContext) {

    // The Initialization of the outgoing set
    Set<MiiPatient> setPatients = ConcurrentHashMap.newKeySet();

    if (!patientIdsCouldBeFound(patientIds, ResourceType.Patient)) {
      return new ArrayList<>();
    }

    // Since the patient (and thus also the encounter) resources are just relevant for statistics
    // of SARS-CoV-2 patient, it reduces the amount of encounter by a lot if its getting
    // prefiltered before
    switch (dataItemContext) {
      case COVID ->
          patientIds =
              handleFilterPatientRetrieval(
                  dataItemContext,
                  this.fhirSearchConfiguration.getFilterPatientRetrieval(),
                  ukbObservations,
                  ukbConditions);
      case INFLUENZA ->
          patientIds =
              handleFilterPatientRetrieval(
                  dataItemContext,
                  this.fhirSearchConfiguration.getInfluenzaFilterPatientRetrieval(),
                  ukbObservations,
                  ukbConditions);
    }

    // Put (unique) pids into a list to facilitate the creation of subsets
    List<String> patientIdList = new ArrayList<>(patientIds);
    List<List<String>> patientIdSublists = splitList(patientIdList, this.getBatchSize());

    AtomicInteger counter = new AtomicInteger(0);
    patientIdSublists.forEach(
        patientSubList -> {
          logStatusDataRetrievalParallel(
              patientIdList.size(), counter.getAndIncrement(), FHIRAllTypes.PATIENT.getDisplay());

          HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();
          if (httpMethod.equals(GET)) {
            reqBundleEntry =
                this.getSearchService()
                    .getBundleData(
                        fhirServerQuerySuffixBuilder.getPatients(this, patientSubList),
                        GET,
                        ResourceType.Patient.name());
          } else if (httpMethod.equals(POST)) {
            reqBundleEntry =
                this.getSearchService()
                    .getBundleData(
                        fhirServerQuerySuffixBuilder.getPatientsPost(this, patientSubList),
                        POST,
                        ResourceType.Patient.name());
          }

          handlePatientRessource(reqBundleEntry, setPatients);
        });
    return new ArrayList<>(setPatients);
  }

  @Override
  public List<MiiPatient> getPatients(Integer maxAgeAtCutOffDate, DataItemContext dataItemContext) {

    Set<MiiPatient> patientsOutput = ConcurrentHashMap.newKeySet();
    LocalDate startingDate;
    // Since the patient (and thus also the encounter) resources are just relevant for statistics
    // of SARS-CoV-2 patient, it reduces the amount of encounter by a lot if its getting
    // prefiltered before
    switch (dataItemContext) {
      case KIDS_RADAR, KIDS_RADAR_PED ->
          startingDate =
              KidsRadar.QUALIFYING_DATE.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
      default ->
          throw new IllegalArgumentException("Unsupported DataItemContext: " + dataItemContext);
    }

    int cutoffYear = startingDate.minusYears(maxAgeAtCutOffDate).getYear();
    int currentYear = LocalDate.now().getYear();

    // Parallel query for each year since cut-off-year till the current year
    IntStream.rangeClosed(cutoffYear, currentYear)
        .parallel()
        .forEach(
            year -> {
              var initialBundle = new Bundle();
              try {
                initialBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getPatients(this, year),
                            GET,
                            ResourceType.Patient.name());
                handlePatientRessource(initialBundle, patientsOutput);

                // Handle pagination for additional pages of patient resources
                while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
                  initialBundle =
                      this.getSearchService()
                          .getBundlePart(
                              getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
                  handlePatientRessource(initialBundle, patientsOutput);
                }
              } catch (Exception e) {
                logErrorRetrieval("Patient query for calendar year " + year, e);
              }
            });

    // The patient ids will be the input filter of further calls in the pipeline
    patientIds =
        patientsOutput.stream()
            .map(PatientIdentifierValueProvider::getPatientId)
            .collect(Collectors.toSet());

    return new ArrayList<>(patientsOutput);
  }

  @Override
  public List<MiiPatient> getPatients(
      List<MiiProcedure> ukbProcedures, List<MiiCondition> ukbConditions) {

    // The Initialization of the outgoing set
    Set<MiiPatient> setPatients = ConcurrentHashMap.newKeySet();

    if (!patientIdsCouldBeFound(patientIds, ResourceType.Patient)) {
      return new ArrayList<>();
    }
    // Put (unique) pids into a list to facilitate the creation of subsets
    List<String> patientIdList =
        Stream.concat(
                ukbProcedures.stream().map(MiiProcedure::getPatientId),
                ukbConditions.stream().map(MiiCondition::getPatientId))
            .distinct()
            .collect(Collectors.toList());

    List<List<String>> patientIdSublists = splitList(patientIdList, this.getBatchSize());

    AtomicInteger counter = new AtomicInteger(0);

    patientIdSublists.forEach(
        patientSubList -> {
          var initialBundle = new Bundle();
          logStatusDataRetrievalParallel(
              patientIdList.size(), counter.getAndIncrement(), FHIRAllTypes.PATIENT.getDisplay());

          HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();
          if (httpMethod.equals(GET)) {
            initialBundle =
                this.getSearchService()
                    .getInitialBundle(
                        fhirServerQuerySuffixBuilder.getPatients(this, patientSubList), GET, null);
          } else if (httpMethod.equals(POST)) {
            initialBundle =
                this.getSearchService()
                    .getInitialBundle(
                        fhirServerQuerySuffixBuilder.getPatientsPost(this, patientSubList),
                        POST,
                        ResourceType.Patient.name());
          }
          handlePatientRessource(initialBundle, setPatients);

          // Handle pagination for additional pages of patient resources
          while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
            initialBundle =
                this.getSearchService()
                    .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
            handlePatientRessource(initialBundle, setPatients);
          }
        });
    return new ArrayList<>(setPatients);
  }

  @Override
  public List<MiiEncounter> getEncounters(DataItemContext dataItemContext) {
    return List.of();
  }

  @Override
  public List<MiiEncounter> getEncounters(
      DataItemContext dataItemContext, List<MiiPatient> patients) {

    // Retrieve ICU-specific configuration for identifying ICU locations via service providers
    Set<String> icuLocationIdsServiceProvider =
        getCustomGlobalConfiguration().getServiceProviderIdentifierOfIcuLocations();
    boolean serviceProviderIdentifierFound = !icuLocationIdsServiceProvider.isEmpty();

    // Check if patient IDs are available for retrieving Encounter resources
    if (patientIdsCouldBeFound(patients, ResourceType.Patient)) {

      // The Initialization of the outgoing set
      Set<MiiEncounter> encounters = ConcurrentHashMap.newKeySet();

      // Reduce to patient id
      Set<String> patientIds = patients.stream().map(MiiPatient::getId).collect(Collectors.toSet());

      // Split the patient IDs into manageable sublists based on the configured batch size
      List<List<String>> patientIdSublists =
          splitList(new ArrayList<>(patientIds), this.getBatchSize());

      // The encounter data retrieval needs to be done one-by-one since each pid will have an
      // individual date.
      AtomicInteger counter = new AtomicInteger(0);
      // TODO Maybe remove IMP + date filter in the fhir search query and do this on code base;
      // could speed up a lot
      patientIdSublists.parallelStream()
          .forEach(
              pidSubList -> {
                var initialBundle = new Bundle();
                logStatusDataRetrievalSequential(
                    0, counter.getAndIncrement(), FHIRAllTypes.ENCOUNTER.getDisplay());

                HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();
                if (httpMethod.equals(GET)) {
                  initialBundle =
                      this.getSearchService()
                          .getInitialBundle(
                              fhirServerQuerySuffixBuilder.getEncounters(
                                  this,
                                  pidSubList,
                                  dataItemContext,
                                  false,
                                  getKickOffDateStringFormat(dataItemContext)),
                              GET,
                              ResourceType.Encounter.name());
                } else if (httpMethod.equals(POST)) {
                  initialBundle =
                      this.getSearchService()
                          .getInitialBundle(
                              fhirServerQuerySuffixBuilder.getEncountersPost(
                                  this,
                                  pidSubList,
                                  dataItemContext,
                                  getKickOffDateStringFormat(dataItemContext)),
                              POST,
                              ResourceType.Encounter.name());
                }
                processEncounterBundle(
                    initialBundle,
                    encounters,
                    icuLocationIdsServiceProvider,
                    serviceProviderIdentifierFound,
                    dataItemContext,
                    false);
                // Handle pagination for additional pages of encounter resources
                while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
                  initialBundle =
                      this.getSearchService()
                          .getBundlePart(
                              getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
                  processEncounterBundle(
                      initialBundle,
                      encounters,
                      icuLocationIdsServiceProvider,
                      serviceProviderIdentifierFound,
                      dataItemContext,
                      false);
                }
              });
      return new ArrayList<>(encounters);
    }

    // Initialize counters for logging and tracking
    AtomicLong countProcessedEncounter =
        new AtomicLong(0); // Tracks the number of processed sublists
    AtomicLong overallTotal = new AtomicLong(0); // Tracks the total number of expected encounters
    Set<MiiEncounter> encounterSet = ConcurrentHashMap.newKeySet();

    // Split the patient IDs into manageable sublists based on the configured batch size
    List<List<String>> patientIdSublists =
        splitList(new ArrayList<>(patientIds), this.getBatchSize());

    // Process each patient sublist in parallel
    patientIdSublists.forEach(
        patientIdSublist -> {
          try {
            // Process a single sublist of patient IDs
            processSublist(
                patientIdSublist,
                dataItemContext,
                encounterSet,
                overallTotal,
                icuLocationIdsServiceProvider,
                serviceProviderIdentifierFound,
                getCustomGlobalConfiguration().getUseOutpatientEncounterWithStatusUnknown());
          } catch (Exception e) {
            // Log any errors encountered while processing the sublist
            log.error(
                "Error processing patient sublist {}: {}", patientIdSublist, e.getMessage(), e);
          }
          // Log progress after processing each sublist
          logStatusDataRetrievalParallel(
              patientIds.size(),
              countProcessedEncounter.getAndIncrement(),
              FHIRAllTypes.ENCOUNTER.getDisplay());
        });

    // Log the total and successfully retrieved encounters
    if (overallTotal.get() != 0)
      log.debug("Tried to retrieve {} encounter resources.", overallTotal.get());
    log.debug("Retrieved {} encounter resources.", encounterSet.size());

    // Warn if there is a mismatch between the expected and retrieved encounter counts
    if (overallTotal.get() != encounterSet.size()
        && fhirSearchConfiguration.getHttpMethod() == GET) {
      log.warn(
          "Mismatch between total and retrieved encounters. {} possibly missing due to batch size"
              + " issues.",
          overallTotal.get() - encounterSet.size());
    }

    // Return the retrieved encounters as a list
    return new ArrayList<>(encounterSet);
  }

  @Override
  public List<MiiEncounter> getEncounters(PidTimestampCohortMap pidTimestampMap) {
    // The Initialization of the outgoing set
    Set<MiiEncounter> encounters = ConcurrentHashMap.newKeySet();

    // Check if patient IDs are available for retrieving Encounter resources
    if (!patientIdsCouldBeFound(pidTimestampMap.keySet(), ResourceType.Consent)) {
      return new ArrayList<>();
    }

    // The encounter data retrieval needs to be done one-by-one since each pid will have an
    // individual date.
    AtomicInteger counter = new AtomicInteger(0);
    pidTimestampMap.forEach(
        (pid, timestamp) -> {
          var initialBundle = new Bundle();
          logStatusDataRetrievalSequential(
              0, counter.getAndIncrement(), FHIRAllTypes.ENCOUNTER.getDisplay());

          HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();
          if (httpMethod.equals(GET)) {
            initialBundle =
                this.getSearchService()
                    .getInitialBundle(
                        fhirServerQuerySuffixBuilder.getEncounters(
                            this, List.of(pid), ACRIBIS, false, dateToFhirSearchSyntax(timestamp)),
                        GET,
                        ResourceType.Encounter.name());
          } else if (httpMethod.equals(POST)) {
            initialBundle =
                this.getSearchService()
                    .getInitialBundle(
                        fhirServerQuerySuffixBuilder.getEncountersPost(
                            this, List.of(pid), ACRIBIS, dateToFhirSearchSyntax(timestamp)),
                        POST,
                        ResourceType.Encounter.name());
          }
          handleEncountersByContext(initialBundle, ACRIBIS, encounters);
          // Handle pagination for additional pages of encounter resources
          while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
            initialBundle =
                this.getSearchService()
                    .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
            handleEncountersByContext(initialBundle, ACRIBIS, encounters);
          }
        });
    return new ArrayList<>(encounters);
  }

  private static void handleEncountersByContext(
      Bundle initialBundle, DataItemContext dataItemContext, Set<MiiEncounter> encounters) {
    initialBundle
        .getEntry()
        .forEach(
            bundleEntry -> {
              // We just need the 'Einrichtungskontakt' resources
              if (bundleEntry.getResource() instanceof Encounter encounter) {
                MiiEncounter miiEncounter = (MiiEncounter) ResourceConverter.convert(encounter);
                // encounter needs to be valid, facility contact and inpatient or post-stationary
                if (miiEncounter.isEncounterStatusValid()) {
                  // By default, add but maybe check other conditions based on context.
                  boolean shouldAdd =
                      switch (dataItemContext) {
                        case ACRIBIS -> isEncounterInpatientFacilityContact(miiEncounter);
                        default -> true;
                      };
                  if (shouldAdd) {
                    encounters.add(miiEncounter);
                  }
                }
              }
            });
  }

  private void processSublist(
      List<String> patientIdSublist,
      DataItemContext dataItemContext,
      Set<MiiEncounter> encounterSet,
      AtomicLong overallTotal,
      Set<String> icuLocationIdsServiceProvider,
      boolean serviceProviderIdentifierFound,
      boolean useOutpatientEncounterWithStatusUnknown) {
    var totalBundle = new Bundle();
    var initialBundle = new Bundle();
    HttpMethod httpMethod =
        fhirSearchConfiguration
            .getHttpMethod(); // Retrieve the first page of encounters for the current sublist
    if (httpMethod.equals(
        GET)) { // Retrieve the total count of encounters for the current patient sublist
      totalBundle =
          this.getSearchService()
              .getInitialBundle(
                  fhirServerQuerySuffixBuilder.getEncounters(
                      this, patientIdSublist, dataItemContext, true, null),
                  GET,
                  null);
      overallTotal.addAndGet(totalBundle.getTotal());

      // Retrieve the first page of encounters for the current sublist
      initialBundle =
          this.getSearchService()
              .getInitialBundle(
                  fhirServerQuerySuffixBuilder.getEncounters(
                      this, patientIdSublist, dataItemContext, false, null),
                  GET,
                  null);
    } else if (httpMethod.equals(POST)) {
      initialBundle =
          this.getSearchService()
              .getInitialBundle(
                  fhirServerQuerySuffixBuilder.getEncountersPost(
                      this, patientIdSublist, dataItemContext, null),
                  POST,
                  ResourceType.Encounter.name());
    }

    processEncounterBundle(
        initialBundle,
        encounterSet,
        icuLocationIdsServiceProvider,
        serviceProviderIdentifierFound,
        dataItemContext,
        useOutpatientEncounterWithStatusUnknown);

    // Handle pagination for additional pages of encounter resources
    while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
      initialBundle =
          this.getSearchService()
              .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
      processEncounterBundle(
          initialBundle,
          encounterSet,
          icuLocationIdsServiceProvider,
          serviceProviderIdentifierFound,
          dataItemContext,
          useOutpatientEncounterWithStatusUnknown);
    }
  }

  private void processEncounterBundle(
      Bundle bundle,
      Set<MiiEncounter> encounterSet,
      Set<String> icuLocationIdsServiceProvider,
      boolean serviceProviderIdentifierFound,
      DataItemContext dataItemContext,
      boolean useOutpatientEncounterWithStatusUnknown) {
    // Process each entry in the bundle
    bundle
        .getEntry()
        .forEach(
            bundleEntry -> {
              if (bundleEntry.getResource() instanceof Encounter encounter) {
                MiiEncounter miiEncounter =
                    (MiiEncounter) ResourceConverter.convert(removeNotNeededAttributes(encounter));
                updateEncounterStatusIfNeeded(
                    useOutpatientEncounterWithStatusUnknown, miiEncounter);
                // Filtering of canceled / entered-in-error encounters
                if (miiEncounter.isEncounterStatusValid()) {
                  // For acribis we just need inpatient cases
                  switch (dataItemContext) {
                    case ACRIBIS -> {
                      if (!miiEncounter.isFacilityContact() && !miiEncounter.isCaseClassInpatient())
                        return;
                    }
                    case KIDS_RADAR -> {
                      if (!miiEncounter.isCaseClassInpatient()) return;
                    }
                  }
                  encounterSet.add(miiEncounter);
                  processEncounterLocations(
                      encounter, icuLocationIdsServiceProvider, serviceProviderIdentifierFound);
                }
              }
            });
  }

  /**
   * Updates the encounter status to {@link EncounterStatus#FINISHED} if the given encounter is an
   * outpatient case and currently has the status {@link EncounterStatus#UNKNOWN}, and the feature
   * flag to allow this update is enabled.
   *
   * @param useOutpatientEncounterWithStatusUnknown flag indicating whether outpatient encounters
   *     with unknown status should be auto-finished
   * @param miiEncounter the encounter object to be checked and potentially updated
   */
  private static void updateEncounterStatusIfNeeded(
      boolean useOutpatientEncounterWithStatusUnknown, MiiEncounter miiEncounter) {

    if (useOutpatientEncounterWithStatusUnknown
        && miiEncounter.isCaseClassOutpatient()
        && miiEncounter.getStatus() == EncounterStatus.UNKNOWN) {
      miiEncounter.setStatus(EncounterStatus.FINISHED);
    }
  }

  private void processConditionBundle(
      Bundle bundle,
      Set<MiiCondition> conditions,
      AtomicInteger filteredConditions,
      DataItemContext dataItemContext) {
    Predicate<MiiCondition> isNeeded = isConditionNeededForContext(dataItemContext);

    bundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .filter(Condition.class::isInstance)
        .map(Condition.class::cast)
        .peek(ResourceHandler::removeNotNeededAttributes)
        .map(c -> (MiiCondition) ResourceConverter.convert(c))
        .forEach(
            ukb -> {
              if (isNeeded.test(ukb)) {
                conditions.add(ukb);
              } else {
                filteredConditions.incrementAndGet();
              }
            });
  }

  private Predicate<MiiCondition> isConditionNeededForContext(DataItemContext dataItemContext) {
    Predicate<MiiCondition> isNeededForKiRaKjp =
        condition -> isConditionNeeded(condition, getKidsRadarIcdCodesKjp());

    Predicate<MiiCondition> isNeededForKiRaPed =
        condition -> isConditionNeeded(condition, getKidsRadarIcdCodesPed());

    // Process each entry in the bundle
    return switch (dataItemContext) {
      case ACRIBIS -> AcribisChecks::isConditionNeededForAcribis;
      case KIDS_RADAR -> isNeededForKiRaKjp.or(isNeededForKiRaPed);
      case KIDS_RADAR_PED -> isNeededForKiRaPed;
      case KIDS_RADAR_KJP -> isNeededForKiRaKjp;
      default ->
          throw new IllegalArgumentException("Unsupported DataItemContext: " + dataItemContext);
    };
  }

  private void processObservationBundle(
      Bundle bundle,
      Set<MiiObservation> observations,
      AtomicInteger filteredConditions,
      DataItemContext dataItemContext,
      QualitativeLabCodesSettings qualitativeLabCodesSettings) {
    // Process each entry in the bundle
    bundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .filter(Observation.class::isInstance)
        .map(Observation.class::cast)
        .peek(ResourceHandler::removeNotNeededAttributes)
        .map(c -> (MiiObservation) ResourceConverter.convert(c))
        .forEach(
            ukb -> {
              if (ObservationFilter.isObservationValueGivenType(
                  ukb, DashboardLogicFixedValues.POSITIVE, qualitativeLabCodesSettings)) {
                observations.add(ukb);
              } else {
                filteredConditions.incrementAndGet();
              }
            });
  }

  private void processEncounterLocations(
      Encounter encounter,
      Set<String> icuLocationIdsServiceProvider,
      boolean serviceProviderIdentifierFound) {
    // Extract location references from the encounter
    encounter
        .getLocation()
        .forEach(
            locEntry -> {
              if (locEntry.hasLocation() && locEntry.getLocation().hasReference()) {
                locationIds.add(extractIdFromReference(locEntry.getLocation()));
              } else {
                log.warn(
                    "Unable to extract location reference for encounter {}", encounter.getId());
              }
            });

    // Add a dummy ICU location if ICU status is flagged by other attributes
    addIcuDummyLocationDueOtherCriteria(
        encounter, serviceProviderIdentifierFound, icuLocationIdsServiceProvider);
  }

  @Override
  public List<MiiProcedure> getProcedures(DataItemContext dataItemContext) {

    // If no case ids could be found, no procedures need to be determined, because the evaluation
    // logic is based on data from the encounter resource.
    if (!encounterIdsCouldBeFound(encounterIds, ResourceType.Procedure)) {
      return new ArrayList<>();
    }

    // Logging progress
    AtomicLong countProcessedProcedures = new AtomicLong(0);

    // Initialization of the outgoing set
    Set<MiiProcedure> setProcedures = ConcurrentHashMap.newKeySet();

    // Sum of all the encounter resources found
    AtomicLong overallTotal = new AtomicLong(0);

    // Input handling
    List<String> patientIdList = new ArrayList<>(patientIds);
    List<List<String>> patientIdSublists = splitList(patientIdList, this.getBatchSize());

    patientIdSublists.parallelStream()
        .forEach(
            patientIdSublist -> {
              var initialBundle = new Bundle();
              var totalBundle = new Bundle();
              HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();
              if (httpMethod.equals(
                  GET)) { // Ask the total count to track if in the end the numbers are the same
                totalBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getProcedures(
                                this,
                                patientIdSublist,
                                null,
                                fhirSearchConfiguration.getProcedureCodesSystemUrl(),
                                true,
                                null,
                                dataItemContext),
                            GET,
                            null);
                initialBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getProcedures(
                                this,
                                patientIdSublist,
                                null,
                                fhirSearchConfiguration.getProcedureCodesSystemUrl(),
                                false,
                                null,
                                dataItemContext),
                            GET,
                            null);
                log.debug("Procedures found for this part bundle: {}", totalBundle.getTotal());
              } else if (httpMethod.equals(POST)) {
                initialBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getProceduresPost(
                                this,
                                patientIdSublist,
                                null,
                                fhirSearchConfiguration.getProcedureCodesSystemUrl(),
                                dataItemContext),
                            POST,
                            ResourceType.Procedure.name());
                log.debug(
                    "Initial procedures found for this part bundle: {}",
                    initialBundle.getEntry().size());
              }
              overallTotal.addAndGet(totalBundle.getTotal());

              // Collecting the encounter resources
              initialBundle
                  .getEntry()
                  .forEach(
                      bundleEntry -> handleProcedureResources(bundleEntry, setProcedures, null));
              // Pagination through follow pages if existing
              while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
                initialBundle =
                    this.getSearchService()
                        .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
                initialBundle
                    .getEntry()
                    .forEach(
                        bundleEntry -> handleProcedureResources(bundleEntry, setProcedures, null));
              }
              logStatusDataRetrievalParallel(
                  patientIdList.size(),
                  countProcessedProcedures.getAndIncrement(),
                  FHIRAllTypes.PROCEDURE.getDisplay());
            });
    return new ArrayList<>(setProcedures);
  }

  @Override
  public List<MiiProcedure> getProcedures(
      Collection<MiiEncounter> encounters, DataItemContext dataItemContext) {

    List<String> facilityContactIds =
        encounters.stream().filter(MiiEncounter::isFacilityContact).map(Resource::getId).toList();

    // If no case ids could be found, no procedures need to be determined, because the evaluation
    // logic is based on data from the encounter resource.
    if (!encounterIdsCouldBeFound(facilityContactIds, ResourceType.Procedure)) {
      return new ArrayList<>();
    }

    // Logging progress
    AtomicLong countProcessedProcedures = new AtomicLong(0);

    // Initialization of the outgoing set
    Set<MiiProcedure> setProcedures = ConcurrentHashMap.newKeySet();

    // Sum of all the encounter resources found
    AtomicLong overallTotal = new AtomicLong(0);

    // Input handling
    List<List<String>> facilityContactsSubList = splitList(facilityContactIds, this.getBatchSize());

    facilityContactsSubList.parallelStream()
        .forEach(
            facilityContactSubList -> {
              var initialBundle = new Bundle();
              var totalBundle = new Bundle();
              HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();
              boolean useGet = httpMethod.equals(GET);
              String resourceType = ResourceType.Procedure.name();

              if (useGet) {
                // GET: fetch total count for comparison
                totalBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getProcedures(
                                this,
                                null,
                                facilityContactSubList,
                                fhirSearchConfiguration.getProcedureCodesSystemUrl(),
                                true,
                                null,
                                dataItemContext),
                            httpMethod,
                            resourceType);

                // GET: load first page
                initialBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getProcedures(
                                this,
                                null,
                                facilityContactSubList,
                                fhirSearchConfiguration.getProcedureCodesSystemUrl(),
                                false,
                                null,
                                dataItemContext),
                            httpMethod,
                            resourceType);

                log.debug("Procedures for this part bundle (total): {}", totalBundle.getTotal());
              } else {
                // POST: load first page via POST body
                initialBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getProceduresPost(
                                this,
                                null,
                                facilityContactSubList,
                                fhirSearchConfiguration.getProcedureCodesSystemUrl(),
                                dataItemContext),
                            httpMethod,
                            resourceType);
                log.debug(
                    "Initial procedures for this part bundle: {}", initialBundle.getEntry().size());
              }
              overallTotal.addAndGet(totalBundle.getTotal());

              // Collecting the encounter resources
              initialBundle
                  .getEntry()
                  .forEach(
                      bundleEntry -> {
                        handleProcedureResources(
                            bundleEntry, setProcedures, AcribisCohortOpsCodes.ALL_CODES);
                      });
              // Pagination through follow pages if existing
              while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
                initialBundle =
                    this.getSearchService()
                        .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
                initialBundle
                    .getEntry()
                    .forEach(
                        bundleEntry ->
                            handleProcedureResources(
                                bundleEntry, setProcedures, AcribisCohortOpsCodes.ALL_CODES));
              }
              logStatusDataRetrievalParallel(
                  facilityContactIds.size(),
                  countProcessedProcedures.getAndIncrement(),
                  FHIRAllTypes.PROCEDURE.getDisplay());
            });
    return new ArrayList<>(setProcedures);
  }

  @Override
  public List<MiiProcedure> getProcedures(
      Collection<MiiEncounter> encounters, DataItemContext dataItemContext, Boolean activeOnly) {

    Set<MiiProcedure> procedures = ConcurrentHashMap.newKeySet();
    Set<String> encounterCaseIds =
        encounters.stream().map(Resource::getId).collect(Collectors.toSet());
    List<List<String>> encounterIdSubsets =
        splitList(new ArrayList<>(encounterCaseIds), this.getBatchSize());
    encounterIdSubsets.parallelStream()
        .forEach(
            subList -> {
              var initialBundle = new Bundle();
              try {
                HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();
                boolean useGet = httpMethod.equals(GET);
                initialBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getProcedures(
                                this,
                                dataItemContext,
                                subList,
                                fhirSearchConfiguration.getProcedureCodesSystemUrl(),
                                useGet,
                                false),
                            httpMethod,
                            ResourceType.Procedure.name());
                switch (dataItemContext) {
                  case KIDS_RADAR -> procedures.addAll(getKiraProceduresFromBundle(initialBundle));
                  default -> throw new Exception("No implemented yet");
                }

                while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
                  initialBundle =
                      this.getSearchService()
                          .getBundlePart(
                              getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
                  switch (dataItemContext) {
                    case KIDS_RADAR ->
                        procedures.addAll(getKiraProceduresFromBundle(initialBundle));
                    default -> throw new Exception("No implemented yet");
                  }
                }
              } catch (Exception e) {
                logErrorRetrieval("Procedure", e);
              }
            });
    return new ArrayList<>(procedures);
  }

  private Collection<? extends MiiProcedure> getKiraProceduresFromBundle(Bundle initialBundle) {
    if (initialBundle == null
        || initialBundle.getEntry() == null
        || initialBundle.getEntry().isEmpty()) {
      return List.of();
    }

    // Stream: pick only Procedure, validate status, convert
    return initialBundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .filter(Procedure.class::isInstance)
        .map(Procedure.class::cast)
        .filter(ResourceHandler::isProcedureStatusValid)
        .map(p -> (MiiProcedure) ResourceConverter.convert(p))
        .filter(
            x ->
                isProcedureNeededForKiRa(
                    x, getIcuProcedureCodesAsList(this, KIDS_RADAR), this.getKjpOpsCodePrefix()))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static void handleProcedureResources(
      BundleEntryComponent bundleEntry, Set<MiiProcedure> setProcedures, List<String> opsCodes) {
    if (bundleEntry.getResource() instanceof Procedure procedure) {
      if (isProcedureStatusValid(procedure)) {
        MiiProcedure ukbProcedure = (MiiProcedure) ResourceConverter.convert(procedure);
        // If a code set is given, just add procedures that hold a given ops code
        if (opsCodes != null && !opsCodes.isEmpty()) {
          if (!FhirConditionTools.isOpsCodeInProcedureWithPrefixWildcardCheck(
              ukbProcedure, opsCodes)) return;
        }
        setProcedures.add((MiiProcedure) ResourceConverter.convert(procedure));
      }
    }
  }

  /**
   * Converts {@link QuestionnaireResponse} resources from a bundle entry and stores them uniquely
   * by ID in the provided map.
   */
  private static void handleQuestionnaireResponseResources(
      BundleEntryComponent bundleEntry,
      Map<String, MiiQuestionnaireResponse> responsesById,
      Set<String> followUpQuestionnaireIds) {
    if (bundleEntry.getResource() instanceof QuestionnaireResponse qr) {
      // Extract the logical ID of the resource
      String id = qr.getIdElement().getIdPart();
      // Skip resources without a valid ID
      if (id == null) {
        return;
      }
      var converted = (MiiQuestionnaireResponse) ResourceConverter.convert(qr);
      // Add only if this ID was not processed before
      responsesById.putIfAbsent(id, converted);
    }
  }

  /**
   * Retrieves the URL for the next page of a paginated FHIR Bundle.
   *
   * @param fhirServerRestConfiguration the FHIR server REST configuration containing potential
   *     prefix settings for pagination.
   * @param bundle the FHIR Bundle object, which contains links for pagination, including the "next"
   *     link.
   * @return the URL for the next page of the FHIR Bundle. If a prefix for pagination is specified
   *     in the FHIR server REST configuration, it is prepended to the "next" URL.
   * @throws NullPointerException if the "next" link is not present in the bundle.
   */
  private static String getNextUrl(
      FhirServerRestConfiguration fhirServerRestConfiguration, Bundle bundle) {
    String fhirServerPrefixPagination = fhirServerRestConfiguration.getPrefixPagination();
    if (fhirServerPrefixPagination != null)
      return fhirServerPrefixPagination + bundle.getLink(NEXT).getUrl();
    return bundle.getLink(NEXT).getUrl();
  }

  @Override
  public List<MiiProcedure> getProcedures(
      List<MiiEncounter> listMiiEncounters,
      List<MiiLocation> listMiiLocations,
      List<MiiObservation> listUkbObservations,
      List<MiiCondition> listUkbConditions,
      DataItemContext dataItemContext) {
    return getProcedures(dataItemContext);
  }

  @Override
  public List<Location> getLocations() {

    // If no case ids could be found, no procedures need to be determined, because the evaluation
    // logic is based on data from the encounter resource.
    if (!locationIdsCouldBeFound(locationIds, ResourceType.Location)) {
      return new ArrayList<>();
    }

    // Initialization of the outgoing set
    Set<Location> setLocations = ConcurrentHashMap.newKeySet();

    List<List<String>> locationIdSublists =
        splitList(new ArrayList<>(locationIds), this.getBatchSize());

    locationIdSublists.forEach(
        locationIdSubList -> {
          HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();
          if (httpMethod.equals(GET)) {
            reqBundleEntry =
                this.getSearchService()
                    .getBundleData(
                        fhirServerQuerySuffixBuilder.getLocations(this, locationIdSubList, true),
                        GET,
                        ResourceType.Location.name());
          } else if (httpMethod.equals(POST)) {
            reqBundleEntry =
                this.getSearchService()
                    .getBundleData(
                        fhirServerQuerySuffixBuilder.getLocations(this, locationIdSubList, false),
                        POST,
                        ResourceType.Location.name());
          }
          reqBundleEntry.forEach(
              bundleEntry -> {
                if (bundleEntry.getResource() instanceof Location location) {
                  setLocations.add(location);
                }
              });
        });
    return new ArrayList<>(setLocations);
  }

  @Override
  public List<MiiConsent> getConsents(Collection<MiiEncounter> miiEncounters) {
    Bundle initialBundle =
        this.getSearchService()
            .getInitialBundle(fhirServerQuerySuffixBuilder.getConsents(this, ACRIBIS), GET, null);
    List<MiiConsent> consents = new ArrayList<>();
    int counter = 0;
    int resourcesTotal = initialBundle.getTotal();

    // Servers like the Blaze do not support the bundle.total, so we retrieve it with an
    // additional fhir search query
    if (!initialBundle.hasTotal()) {
      resourcesTotal =
          this.getSearchService()
              .getInitialBundle(fhirServerQuerySuffixBuilder.getConsents(this, ACRIBIS), GET, null)
              .getTotal();
    }

    while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
      // The handling differs, whether the output is condition resources only or the encounter
      // data needs to be retrieved aswell.
      // -> Then we need to gather all resources first and make encounter id processing afterwards.
      // Parsing the retrieved resources and reading out the patients and Encounter Ids for later
      // data queries.
      ResourceHandler.handleConsentEntries(
          initialBundle, consents, miiEncounters, patientIds, this.getServerType());
      initialBundle =
          this.getSearchService()
              .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
      logStatusDataRetrievalSequential(
          resourcesTotal, counter++, FHIRAllTypes.CONSENT.getDisplay());
    }
    // no next link existent? -> just add the results to the lists and go further on
    ResourceHandler.handleConsentEntries(
        initialBundle, consents, miiEncounters, patientIds, this.getServerType());
    return consents;
  }

  @Override
  public List<MiiConsent> getConsents() {
    Bundle initialBundle =
        this.getSearchService()
            .getInitialBundle(fhirServerQuerySuffixBuilder.getConsents(this, BCT), GET, null);
    List<MiiConsent> broadConsents = new ArrayList<>();
    int counter = 0;
    int resourcesTotal = initialBundle.getTotal();

    while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
      // The handling differs, whether the output is condition resources only or the encounter
      // data needs to be retrieved as well.
      // -> Then we need to gather all resources first and make encounter id processing afterward.
      // Parsing the retrieved resources and reading out the patients and Encounter Ids for later
      // data queries.
      ResourceHandler.handleBroadConsentEntries(initialBundle, broadConsents, this.getServerType());
      initialBundle =
          this.getSearchService()
              .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
      logStatusDataRetrievalSequential(
          resourcesTotal, counter++, FHIRAllTypes.CONSENT.getDisplay());
    }
    // no next link existent? -> just add the results to the lists and go further on
    ResourceHandler.handleBroadConsentEntries(initialBundle, broadConsents, this.getServerType());
    return broadConsents;
  }

  Set<String> followUpQuestionnaireIds = new HashSet<>();

  @Override
  public List<MiiQuestionnaireResponse> getQuestionnaireResponses(List<String> patientIds) {

    // If no patient resource ids could be found, no procedures need to be determined
    if (!patientIdsCouldBeFound(patientIds, ResourceType.QuestionnaireResponse)) {
      return new ArrayList<>();
    }

    // Logging progress
    AtomicLong countProcessedQrs = new AtomicLong(0);
    // Initialization of the outgoing set
    Map<String, MiiQuestionnaireResponse> followUpQRsById = new ConcurrentHashMap<>();
    // Collecting the qr resources
    AtomicLong overallTotal = new AtomicLong(0);
    // Input handling
    List<List<String>> patientIdSubLists = splitList(patientIds, this.getBatchSize());

    followUpQuestionnaireIds = new HashSet<>();

    patientIdSubLists.parallelStream()
        .forEach(
            patientIdSubList -> {
              var initialBundle = new Bundle();
              var totalBundle = new Bundle();
              HttpMethod httpMethod = fhirSearchConfiguration.getHttpMethod();
              boolean useGet = httpMethod.equals(GET);
              String resourceTypeQr = ResourceType.QuestionnaireResponse.name();
              String resourceTypeQ = ResourceType.Questionnaire.name();

              if (useGet) {
                // TODO remove if not needed
                // First: Figure out the ids of the acribis questionnaires
                //                Bundle questionnaires =
                //                    this.getSearchService()
                //                        .getInitialBundle(
                //                            fhirServerQuerySuffixBuilder.getQuestionnaires(this,
                // true),
                //                            httpMethod,
                //                            resourceTypeQ);
                //               followUpQuestionnaireIds =
                //                    questionnaires.getEntry().stream()
                //                        .map(entry ->  entry.getResource().getIdPart())
                //                        .collect(Collectors.toSet());

                // GET: fetch total count for comparison
                totalBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getQuestionnaireResponses(
                                this, patientIdSubList, followUpQuestionnaireIds, true),
                            httpMethod,
                            resourceTypeQr);

                // GET: load first page
                initialBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getQuestionnaireResponses(
                                this, patientIdSubList, followUpQuestionnaireIds, true),
                            httpMethod,
                            resourceTypeQr);
                log.debug(
                    "QuestionnaireResponses for this part bundle (total): {}",
                    totalBundle.getTotal());
              } else {

                // First: Figure out the ids of the acribis questionnaires
                //                Bundle questionnaires =
                //                    this.getSearchService()
                //                        .getInitialBundle(
                //                            fhirServerQuerySuffixBuilder.getQuestionnaires(this,
                // false),
                //                            httpMethod,
                //                            resourceTypeQ);
                //               followUpQuestionnaireIds =
                //                    questionnaires.getEntry().stream()
                //                        .map(entry -> "Questionnaire/"+
                // entry.getResource().getIdPart())
                //                        .collect(Collectors.toSet());

                // POST: load first page via POST body
                initialBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getQuestionnaireResponses(
                                this, patientIdSubList, followUpQuestionnaireIds, false),
                            httpMethod,
                            resourceTypeQr);
                log.debug(
                    "Initial QuestionnaireResponses for this part bundle: {}",
                    initialBundle.getEntry().size());
              }
              overallTotal.addAndGet(totalBundle.getTotal());

              // Collecting the qr resources
              initialBundle
                  .getEntry()
                  .forEach(
                      bundleEntry -> {
                        handleQuestionnaireResponseResources(
                            bundleEntry, followUpQRsById, followUpQuestionnaireIds);
                      });
              // Pagination through follow pages if existing
              while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
                initialBundle =
                    this.getSearchService()
                        .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
                initialBundle
                    .getEntry()
                    .forEach(
                        bundleEntry ->
                            handleQuestionnaireResponseResources(
                                bundleEntry, followUpQRsById, followUpQuestionnaireIds));
              }
              logStatusDataRetrievalParallel(
                  patientIds.size(),
                  countProcessedQrs.getAndIncrement(),
                  FHIRAllTypes.QUESTIONNAIRERESPONSE.getDisplay());
            });

    return new ArrayList<>(followUpQRsById.values());
  }

  /**
   * Providing status information about the current retrieval of FHIR resources for which the output
   * quantity is not determined before sending the FHIR Search Query (e.g. retrieval of all
   * Observation Resources with Loinc code 12345-6).
   *
   * @param totalEntries Number of resources in the {@link Bundle}
   * @param counter Current counter of the processing resources (this value is not incremented in
   *     this method)
   * @param resourceType Resource type that is being processed
   */
  private void logStatusDataRetrievalSequential(
      int totalEntries, int counter, String resourceType) {
    if (counter > 0 && counter % 10 == 0) {
      logger.info(
          "Retrieving {} data: {}/{}",
          resourceType,
          counter * this.getBatchSize(),
          totalEntries != 0 ? totalEntries : "?");
    }
  }

  /**
   * Providing status information about the current retrieval of FHIR resources for which the output
   * quantity is determined before the FHIR search query is sent (e.g. retrieval of all patient
   * resources via supplied corresponding IDs) and where the queries can be parallelized.
   *
   * @param inputParameterSize The total number of input IDs for which resources are retrieved
   * @param counter Current counter of the processing resources (this value is not incremented in
   *     this method)
   * @param resourceType Resource type that is being processed
   */
  private void logStatusDataRetrievalParallel(
      int inputParameterSize, long counter, String resourceType) {
    if ((counter * this.getBatchSize()) % 10000 == 0) {
      logger.info(
          "Retrieving {} data: {}/{} patients",
          resourceType,
          counter * this.getBatchSize(),
          inputParameterSize);
    }
  }

  public ServerTypeEnum getServerType() {
    return ServerTypeEnum.FHIR;
  }

  public int getBatchSize() {
    return this.fhirSearchConfiguration.getBatchSize();
  }

  @Override
  public List<Encounter> getIcuEncounters() throws SearchException {
    return null;
  }

  @Override
  public List<CoreBaseDataItem> getIcuEpisodes(Collection<String> caseIds) throws SearchException {
    return null;
  }

  @Override
  public List<CoreBaseDataItem> getUkbRenalReplacementObservations(
      Collection<String> encounterIds, Set<Integer> codes) {
    return null;
  }

  @Override
  public List<CoreBaseDataItem> getUkbRenalReplacementBodyWeight(
      Collection<String> encounterIds, DataSourceType dataSourceType) {
    return null;
  }

  @Override
  public List<CoreBaseDataItem> getUkbRenalReplacementStart(
      Collection<String> icuLocalCaseIds, DataSourceType dataSourceType) {
    return null;
  }

  @Override
  public List<CoreBaseDataItem> getUkbRenalReplacementUrineOutput(
      Collection<String> icuLocalCaseIds, DataSourceType dataSourceType) {
    return null;
  }

  @Override
  public CapabilityStatement getStatus() {
    return this.getSearchService().getCapabilityStatement("metadata", GET, null);
  }
}
