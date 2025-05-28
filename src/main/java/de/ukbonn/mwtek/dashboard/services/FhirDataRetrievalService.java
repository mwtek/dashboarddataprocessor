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

import static de.ukbonn.mwtek.dashboard.misc.AcribisChecks.isConditionNeededForAcribis;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_CONDITIONS;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_OBSERVATIONS_PCR;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_OBSERVATIONS_VARIANTS;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_PROCEDURES_ECMO;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_PROCEDURES_VENTILATION;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.INFLUENZA_CONDITIONS;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.INFLUENZA_OBSERVATIONS_PCR;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.extractInputCodes;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.extractKidsRadarDiagnosisConditions;
import static de.ukbonn.mwtek.dashboard.misc.IcuStayDetection.addIcuDummyLocationDueOtherCriteria;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.encounterIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.locationIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.patientIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboard.misc.ResourceHandler.isEncounterInpatientFacilityContact;
import static de.ukbonn.mwtek.dashboard.misc.ResourceHandler.isProcedureStatusValid;
import static de.ukbonn.mwtek.dashboard.misc.ResourceHandler.removeNotNeededAttributes;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.ACRIBIS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.KJP;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.RSV;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.extractIdFromReference;
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
import de.ukbonn.mwtek.dashboard.misc.FhirServerQuerySuffixBuilder;
import de.ukbonn.mwtek.dashboard.misc.ResourceHandler;
import de.ukbonn.mwtek.dashboardlogic.enums.AcribisCohortOpsCodes;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.PidTimestampCohortMap;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.misc.FhirConditionTools;
import de.ukbonn.mwtek.utilities.fhir.misc.ResourceConverter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbConsent;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import de.ukbonn.mwtek.utilities.generic.collections.ListTools;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  List<Bundle.BundleEntryComponent> reqBundleEntry = null;

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
        extractKidsRadarDiagnosisConditions(customGlobalConfiguration, RSV));
  }

  @Override
  public List<Observation> getObservations(DataItemContext dataItemContext) {

    Bundle initialBundle =
        this.getSearchService()
            .getInitialBundle(
                fhirServerQuerySuffixBuilder.getObservations(this, null, false, dataItemContext),
                GET,
                null);
    List<Observation> listObservations = new ArrayList<>();
    int resourcesTotal = initialBundle.getTotal();

    // Servers like the Blaze do not support the bundle.total, so we retrieve it with an
    // additional fhir search query
    if (!initialBundle.hasTotal()) {
      resourcesTotal =
          this.getSearchService()
              .getInitialBundle(
                  fhirServerQuerySuffixBuilder.getObservations(this, null, true, dataItemContext),
                  GET,
                  null)
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
          resourcesTotal, counterObs++, Enumerations.FHIRAllTypes.OBSERVATION.getDisplay());
    }
    // No next link given? -> just add the results to the lists and go on
    ResourceHandler.handleObservationEntries(
        initialBundle, listObservations, patientIds, encounterIds, this.getServerType());

    return listObservations;
  }

  @Override
  public List<Condition> getConditions(DataItemContext dataItemContext) {

    boolean isUseEncounterConditionReference =
        fhirSearchConfiguration.isUseEncounterConditionReference();
    // Since the condition.encounter reference is not mandatory, it's possible to link encounter
    // and condition via encounter.diagnosis.
    Bundle initialBundle =
        !isUseEncounterConditionReference
            ? this.getSearchService()
                .getInitialBundle(
                    fhirServerQuerySuffixBuilder.getConditions(this, null, false, dataItemContext),
                    GET,
                    null)
            : this.getSearchService()
                .getInitialBundle(
                    fhirServerQuerySuffixBuilder.getConditionsIncludingEncounter(
                        this, dataItemContext),
                    GET,
                    null);
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
                  fhirServerQuerySuffixBuilder.getObservations(this, null, true, dataItemContext),
                  GET,
                  null)
              .getTotal();
    }

    while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
      // The handling differs, whether the output is condition resources only or the encounter
      // data needs to be retrieved aswell.
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
              .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), GET);

      logStatusDataRetrievalSequential(
          resourcesTotal, counterCond++, Enumerations.FHIRAllTypes.CONDITION.getDisplay());
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
  public List<UkbCondition> getConditions(Collection<UkbEncounter> encounters)
      throws RestClientException, OutOfMemoryError {
    Set<UkbCondition> setConditions = ConcurrentHashMap.newKeySet();
    Set<String> encounterCaseIds =
        encounters.stream().map(Resource::getId).collect(Collectors.toSet());
    AtomicInteger filteredConditions = new AtomicInteger(0);
    List<List<String>> encounterIdSubsets =
        ListTools.splitList(new ArrayList<>(encounterCaseIds), this.getBatchSize());
    encounterIdSubsets.parallelStream()
        .forEach(
            subList -> {
              var initialBundle = new Bundle();
              try {
                switch (fhirSearchConfiguration.getHttpMethod()) {
                  case GET ->
                      initialBundle =
                          this.getSearchService()
                              .getInitialBundle(
                                  fhirServerQuerySuffixBuilder.getConditions(this, subList),
                                  GET,
                                  ResourceType.Condition.name());
                  case POST ->
                      initialBundle =
                          this.getSearchService()
                              .getInitialBundle(
                                  fhirServerQuerySuffixBuilder.getConditionsPost(this, subList),
                                  POST,
                                  ResourceType.Condition.name());
                }

                processConditionBundle(initialBundle, setConditions, filteredConditions);

                while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
                  initialBundle =
                      this.getSearchService()
                          .getBundlePart(
                              getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
                  processConditionBundle(initialBundle, setConditions, filteredConditions);
                }
              } catch (Exception e) {
                logErrorRetrieval("Condition", e);
              }
            });

    log.debug(
        "{} condition resources got filtered because no needed icd code was found.",
        filteredConditions.get());
    return new ArrayList<>(setConditions);
  }

  @Override
  public List<Patient> getPatients(
      List<UkbObservation> ukbObservations,
      List<UkbCondition> ukbConditions,
      DataItemContext dataItemContext) {

    // The Initialization of the outgoing set
    Set<Patient> setPatients = ConcurrentHashMap.newKeySet();

    if (!patientIdsCouldBeFound(patientIds, ResourceType.Patient)) {
      return new ArrayList<>();
    }

    // Since the patient (and thus also the encounter) resources are just relevant for statistics
    // of SARS-CoV-2 patient it reduces the amount of encounter by a lot if its getting
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
              patientIdList.size(),
              counter.getAndIncrement(),
              Enumerations.FHIRAllTypes.PATIENT.getDisplay());

          switch (fhirSearchConfiguration.getHttpMethod()) {
            case GET ->
                reqBundleEntry =
                    this.getSearchService()
                        .getBundleData(
                            fhirServerQuerySuffixBuilder.getPatients(this, patientSubList),
                            GET,
                            null);
            case POST ->
                reqBundleEntry =
                    this.getSearchService()
                        .getBundleData(
                            fhirServerQuerySuffixBuilder.getPatientsPost(this, patientSubList),
                            POST,
                            ResourceType.Patient.name());
          }

          reqBundleEntry.parallelStream()
              .forEach(singlePatient -> setPatients.add((Patient) singlePatient.getResource()));
        });
    return new ArrayList<>(setPatients);
  }

  @Override
  public List<Patient> getPatients(
      List<UkbProcedure> ukbProcedures, List<UkbCondition> ukbConditions) {

    // The Initialization of the outgoing set
    Set<Patient> setPatients = ConcurrentHashMap.newKeySet();

    if (!patientIdsCouldBeFound(patientIds, ResourceType.Patient)) {
      return new ArrayList<>();
    }
    // Put (unique) pids into a list to facilitate the creation of subsets
    List<String> patientIdList =
        Stream.concat(
                ukbProcedures.stream().map(UkbProcedure::getPatientId),
                ukbConditions.stream().map(UkbCondition::getPatientId))
            .distinct()
            .collect(Collectors.toList());

    List<List<String>> patientIdSublists = splitList(patientIdList, this.getBatchSize());

    AtomicInteger counter = new AtomicInteger(0);
    patientIdSublists.forEach(
        patientSubList -> {
          logStatusDataRetrievalParallel(
              patientIdList.size(),
              counter.getAndIncrement(),
              Enumerations.FHIRAllTypes.PATIENT.getDisplay());

          switch (fhirSearchConfiguration.getHttpMethod()) {
            case GET ->
                reqBundleEntry =
                    this.getSearchService()
                        .getBundleData(
                            fhirServerQuerySuffixBuilder.getPatients(this, patientSubList),
                            GET,
                            null);
            case POST ->
                reqBundleEntry =
                    this.getSearchService()
                        .getBundleData(
                            fhirServerQuerySuffixBuilder.getPatientsPost(this, patientSubList),
                            POST,
                            ResourceType.Patient.name());
          }

          reqBundleEntry.parallelStream()
              .forEach(singlePatient -> setPatients.add((Patient) singlePatient.getResource()));
        });
    return new ArrayList<>(setPatients);
  }

  @Override
  public List<UkbEncounter> getEncounters(DataItemContext dataItemContext) {
    // Check if patient IDs are available for retrieving Encounter resources
    if (!patientIdsCouldBeFound(patientIds, ResourceType.Encounter)) {
      return new ArrayList<>();
    }

    // Initialize counters for logging and tracking
    AtomicLong countProcessedEncounter =
        new AtomicLong(0); // Tracks the number of processed sublists
    AtomicLong overallTotal = new AtomicLong(0); // Tracks the total number of expected encounters
    Set<UkbEncounter> encounterSet = ConcurrentHashMap.newKeySet();

    // Retrieve ICU-specific configuration for identifying ICU locations via service providers
    Set<String> icuLocationIdsServiceProvider =
        getCustomGlobalConfiguration().getServiceProviderIdentifierOfIcuLocations();
    boolean serviceProviderIdentifierFound = !icuLocationIdsServiceProvider.isEmpty();

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
                serviceProviderIdentifierFound);
          } catch (Exception e) {
            // Log any errors encountered while processing the sublist
            log.error(
                "Error processing patient sublist {}: {}", patientIdSublist, e.getMessage(), e);
          }
          // Log progress after processing each sublist
          logStatusDataRetrievalParallel(
              patientIds.size(),
              countProcessedEncounter.getAndIncrement(),
              Enumerations.FHIRAllTypes.ENCOUNTER.getDisplay());
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
  public List<UkbEncounter> getEncounters(PidTimestampCohortMap pidTimestampMap) {
    // The Initialization of the outgoing set
    Set<UkbEncounter> encounters = ConcurrentHashMap.newKeySet();

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

          switch (fhirSearchConfiguration.getHttpMethod()) {
            case GET ->
                initialBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getEncounters(
                                this,
                                List.of(pid),
                                ACRIBIS,
                                false,
                                dateToFhirSearchSyntax(timestamp)),
                            GET,
                            ResourceType.Encounter.name());
            case POST ->
                initialBundle =
                    this.getSearchService()
                        .getInitialBundle(
                            fhirServerQuerySuffixBuilder.getEncountersPost(
                                this, List.of(pid), ACRIBIS, dateToFhirSearchSyntax(timestamp)),
                            POST,
                            ResourceType.Encounter.name());
          }
          handleAcribisEncounters(initialBundle, encounters);
          // Handle pagination for additional pages of encounter resources
          while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
            initialBundle =
                this.getSearchService()
                    .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
            handleAcribisEncounters(initialBundle, encounters);
          }
        });
    return new ArrayList<>(encounters);
  }

  private static void handleAcribisEncounters(Bundle initialBundle, Set<UkbEncounter> encounters) {
    initialBundle
        .getEntry()
        .forEach(
            bundleEntry -> {
              // We just need the 'Einrichtungskontakt' resources
              if (bundleEntry.getResource() instanceof Encounter encounter) {
                UkbEncounter ukbEncounter = (UkbEncounter) ResourceConverter.convert(encounter);
                // encounter needs to be valid, facility contact and inpatient or post-stationary
                if (isEncounterStatusValid(ukbEncounter)
                    && isEncounterInpatientFacilityContact(ukbEncounter))
                  encounters.add(ukbEncounter);
              }
            });
  }

  private void processSublist(
      List<String> patientIdSublist,
      DataItemContext dataItemContext,
      Set<UkbEncounter> encounterSet,
      AtomicLong overallTotal,
      Set<String> icuLocationIdsServiceProvider,
      boolean serviceProviderIdentifierFound) {
    var totalBundle = new Bundle();
    var initialBundle = new Bundle();
    switch (fhirSearchConfiguration.getHttpMethod()) {
      case GET -> { // Retrieve the total count of encounters for the current patient sublist
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
      }
      case POST -> // Retrieve the first page of encounters for the current sublist
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
        dataItemContext);

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
          dataItemContext);
    }
  }

  private void processEncounterBundle(
      Bundle bundle,
      Set<UkbEncounter> encounterSet,
      Set<String> icuLocationIdsServiceProvider,
      boolean serviceProviderIdentifierFound,
      DataItemContext dataItemContext) {
    // Process each entry in the bundle
    bundle
        .getEntry()
        .forEach(
            bundleEntry -> {
              if (bundleEntry.getResource() instanceof Encounter encounter) {
                // Filtering of canceled / entered-in-error encounters
                if (isEncounterStatusValid(encounter)) {
                  UkbEncounter ukbEncounter =
                      (UkbEncounter)
                          ResourceConverter.convert(removeNotNeededAttributes(encounter));
                  // For acribis we just need inpatient cases
                  if (dataItemContext == KIDS_RADAR) {
                    if (!ukbEncounter.isFacilityContact() && !ukbEncounter.isCaseClassInpatient())
                      return;
                  }
                  encounterSet.add(ukbEncounter);

                  processEncounterLocations(
                      encounter, icuLocationIdsServiceProvider, serviceProviderIdentifierFound);
                }
              }
            });
  }

  private void processConditionBundle(
      Bundle bundle, Set<UkbCondition> encounterSet, AtomicInteger filteredConditions) {
    // Process each entry in the bundle
    bundle
        .getEntry()
        .forEach(
            bundleEntry -> {
              if (bundleEntry.getResource() instanceof Condition condition) {
                removeNotNeededAttributes(condition);
                UkbCondition ukbCondition = (UkbCondition) ResourceConverter.convert(condition);
                if (isConditionNeededForAcribis(ukbCondition)) {
                  encounterSet.add(ukbCondition);
                } else filteredConditions.getAndIncrement();
              }
            });
  }

  /**
   * Filtering of non-usable encounters, for example, if they got canceled or entered in error.
   *
   * <p>Since many data items rely explicit on {@link EncounterStatus#INPROGRESS) oder {@link
   * EncounterStatus#FINISHED}} we filter already to the corresponding values.
   */
  private static boolean isEncounterStatusValid(Encounter encounter) {
    if (!encounter.hasStatus()) return false;
    else {
      var encounterStatus = encounter.getStatus();
      return (encounterStatus == EncounterStatus.INPROGRESS
          || encounterStatus == EncounterStatus.FINISHED);
    }
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
  public List<UkbProcedure> getProcedures(DataItemContext dataItemContext) {

    // If no case ids could be found, no procedures need to be determined, because the evaluation
    // logic is based on data from the encounter resource.
    if (!encounterIdsCouldBeFound(encounterIds, ResourceType.Procedure)) {
      return new ArrayList<>();
    }

    // Logging progress
    AtomicLong countProcessedProcedures = new AtomicLong(0);

    // Initialization of the outgoing set
    Set<UkbProcedure> setProcedures = ConcurrentHashMap.newKeySet();

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
              switch (fhirSearchConfiguration.getHttpMethod()) {
                case GET -> {
                  // Ask the total count to track if in the end the numbers are the same
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
                }
                case POST -> {
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
              }
              overallTotal.addAndGet(totalBundle.getTotal());

              // Collecting the encounter resources
              initialBundle
                  .getEntry()
                  .forEach(
                      bundleEntry -> {
                        handleProcedureResources(bundleEntry, setProcedures, null);
                      });
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
                  Enumerations.FHIRAllTypes.PROCEDURE.getDisplay());
            });
    return new ArrayList<>(setProcedures);
  }

  @Override
  public List<UkbProcedure> getProcedures(
      Collection<UkbEncounter> encounters, DataItemContext dataItemContext) {

    List<String> facilityContactIds =
        encounters.stream().filter(UkbEncounter::isFacilityContact).map(Resource::getId).toList();

    // If no case ids could be found, no procedures need to be determined, because the evaluation
    // logic is based on data from the encounter resource.
    if (!encounterIdsCouldBeFound(facilityContactIds, ResourceType.Procedure)) {
      return new ArrayList<>();
    }

    // Logging progress
    AtomicLong countProcessedProcedures = new AtomicLong(0);

    // Initialization of the outgoing set
    Set<UkbProcedure> setProcedures = ConcurrentHashMap.newKeySet();

    // Sum of all the encounter resources found
    AtomicLong overallTotal = new AtomicLong(0);

    // Input handling
    List<List<String>> facilityContactsSubList = splitList(facilityContactIds, this.getBatchSize());

    facilityContactsSubList.parallelStream()
        .forEach(
            facilityContactSubList -> {
              var initialBundle = new Bundle();
              var totalBundle = new Bundle();
              switch (fhirSearchConfiguration.getHttpMethod()) {
                case GET -> {
                  // Ask the total count to track if in the end the numbers are the same
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
                              GET,
                              null);
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
                              GET,
                              null);
                  log.debug("Procedures found for this part bundle: {}", totalBundle.getTotal());
                }
                case POST -> {
                  initialBundle =
                      this.getSearchService()
                          .getInitialBundle(
                              fhirServerQuerySuffixBuilder.getProceduresPost(
                                  this,
                                  null,
                                  facilityContactSubList,
                                  fhirSearchConfiguration.getProcedureCodesSystemUrl(),
                                  dataItemContext),
                              POST,
                              ResourceType.Procedure.name());
                  log.debug(
                      "Initial procedures found for this part bundle: {}",
                      initialBundle.getEntry().size());
                }
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
                  Enumerations.FHIRAllTypes.PROCEDURE.getDisplay());
            });
    return new ArrayList<>(setProcedures);
  }

  private static void handleProcedureResources(
      BundleEntryComponent bundleEntry, Set<UkbProcedure> setProcedures, List<String> opsCodes) {
    if (bundleEntry.getResource() instanceof Procedure procedure) {
      if (isProcedureStatusValid(procedure)) {
        UkbProcedure ukbProcedure = (UkbProcedure) ResourceConverter.convert(procedure);
        // If a code set is given, just add procedures that hold a given ops code
        if (opsCodes != null && !opsCodes.isEmpty()) {
          if (!FhirConditionTools.isOpsCodeInProcedureWithPrefixWildcardCheck(
              ukbProcedure, opsCodes)) return;
        }
        setProcedures.add((UkbProcedure) ResourceConverter.convert(procedure));
      }
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
  public List<UkbProcedure> getProcedures(
      List<UkbEncounter> listUkbEncounters,
      List<UkbLocation> listUkbLocations,
      List<UkbObservation> listUkbObservations,
      List<UkbCondition> listUkbConditions,
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
          switch (fhirSearchConfiguration.getHttpMethod()) {
            case GET ->
                reqBundleEntry =
                    this.getSearchService()
                        .getBundleData(
                            fhirServerQuerySuffixBuilder.getLocations(this, locationIdSubList),
                            GET,
                            null);
            case POST ->
                reqBundleEntry =
                    this.getSearchService()
                        .getBundleData(
                            fhirServerQuerySuffixBuilder.getLocationsPost(this, locationIdSubList),
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
  public List<UkbConsent> getConsents(Collection<UkbEncounter> ukbEncounters) {
    Bundle initialBundle =
        this.getSearchService()
            .getInitialBundle(fhirServerQuerySuffixBuilder.getConsents(this, ACRIBIS), GET, null);
    List<UkbConsent> consents = new ArrayList<>();
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
          initialBundle, consents, ukbEncounters, patientIds, this.getServerType());
      initialBundle =
          this.getSearchService()
              .getBundlePart(getNextUrl(fhirServerRestConfiguration, initialBundle), GET);
      logStatusDataRetrievalSequential(
          resourcesTotal, counter++, FHIRAllTypes.CONSENT.getDisplay());
    }
    // no next link existent? -> just add the results to the lists and go further on
    ResourceHandler.handleConsentEntries(
        initialBundle, consents, ukbEncounters, patientIds, this.getServerType());
    return consents;
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
}
