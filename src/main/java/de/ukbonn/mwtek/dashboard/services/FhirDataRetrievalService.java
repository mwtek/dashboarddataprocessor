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
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.extractInputCodes;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.extractKidsRadarDiagnosisConditions;
import static de.ukbonn.mwtek.dashboard.misc.IcuStayDetection.addIcuDummyLocationDueOtherCriteria;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.encounterIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.locationIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.patientIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.KJP;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.RSV;
import static de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality.extractIdFromReference;
import static de.ukbonn.mwtek.utilities.generic.collections.ListTools.splitList;

import de.ukbonn.mwtek.dashboard.DashboardApplication;
import de.ukbonn.mwtek.dashboard.configuration.FhirSearchConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.GlobalConfiguration;
import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import de.ukbonn.mwtek.dashboard.exceptions.SearchException;
import de.ukbonn.mwtek.dashboard.interfaces.DataSourceType;
import de.ukbonn.mwtek.dashboard.interfaces.SearchService;
import de.ukbonn.mwtek.dashboard.misc.FhirServerQuerySuffixBuilder;
import de.ukbonn.mwtek.dashboard.misc.ResourceHandler;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  Logger logger = LoggerFactory.getLogger(DashboardApplication.class);

  /** List with the entries of the bundles returned per FHIR search query */
  List<Bundle.BundleEntryComponent> reqBundleEntry = null;

  public FhirDataRetrievalService(
      SearchService searchService,
      FhirSearchConfiguration fhirSearchConfiguration,
      GlobalConfiguration globalConfiguration) {
    this.fhirSearchConfiguration = fhirSearchConfiguration;
    initializeSearchService(searchService, fhirSearchConfiguration, globalConfiguration);
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
      GlobalConfiguration globalConfiguration) {
    this.setSearchService(searchService);
    this.setSearchConfiguration(fhirSearchConfiguration);
    this.setGlobalConfiguration(globalConfiguration);
    // Read the Sars-Cov2 PCR codes from the configuration and persist them in a list analogous
    // to the Acuwave parameterization (FHIR servers expect a comma separated list of strings
    // while the Acuwave needs an integer list)
    this.setCovidLabPcrCodes(extractInputCodes(globalConfiguration, COVID_OBSERVATIONS_PCR));
    this.setCovidLabVariantCodes(
        extractInputCodes(globalConfiguration, COVID_OBSERVATIONS_VARIANTS));
    this.setProcedureVentilationCodes(
        extractInputCodes(globalConfiguration, COVID_PROCEDURES_VENTILATION));
    this.setProcedureEcmoCodes(extractInputCodes(globalConfiguration, COVID_PROCEDURES_ECMO));
    // Reading of the icd codes from the configuration and transforming it into a list
    this.setCovidIcdCodes(extractInputCodes(globalConfiguration, COVID_CONDITIONS));
    // Influenza data setter
    this.setInfluenzaIcdCodes(extractInputCodes(globalConfiguration, INFLUENZA_CONDITIONS));
    this.setInfluenzaLabPcrCodes(
        extractInputCodes(globalConfiguration, INFLUENZA_OBSERVATIONS_PCR));
    this.setKidsRadarKjpIcdCodes(extractKidsRadarDiagnosisConditions(globalConfiguration, KJP));
    this.setKidsRadarRsvIcdCodes(extractKidsRadarDiagnosisConditions(globalConfiguration, RSV));
  }

  @Override
  public List<Observation> getObservations(DataItemContext dataItemContext) {

    Bundle initialBundle =
        this.getSearchService()
            .getInitialBundle(
                fhirServerQuerySuffixBuilder.getObservations(this, null, false, dataItemContext));
    List<Observation> listObservations = new ArrayList<>();
    int resourcesTotal = initialBundle.getTotal();

    // Servers like the Blaze do not support the bundle.total, so we retrieve it with an
    // additional fhir search query
    if (!initialBundle.hasTotal()) {
      resourcesTotal =
          this.getSearchService()
              .getInitialBundle(
                  fhirServerQuerySuffixBuilder.getObservations(this, null, true, dataItemContext))
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
      initialBundle = this.getSearchService().getBundlePart(initialBundle.getLink(NEXT).getUrl());

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
                    fhirServerQuerySuffixBuilder.getConditions(this, null, false, dataItemContext))
            : this.getSearchService()
                .getInitialBundle(
                    fhirServerQuerySuffixBuilder.getConditionsIncludingEncounter(
                        this, dataItemContext));
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
                  fhirServerQuerySuffixBuilder.getObservations(this, null, true, dataItemContext))
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
      initialBundle = this.getSearchService().getBundlePart(initialBundle.getLink(NEXT).getUrl());

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

          reqBundleEntry =
              this.getSearchService()
                  .getBundleData(fhirServerQuerySuffixBuilder.getPatients(this, patientSubList));

          reqBundleEntry.parallelStream()
              .forEach(singlePatient -> setPatients.add((Patient) singlePatient.getResource()));
        });
    return new ArrayList<>(setPatients);
  }

  @Override
  public List<Encounter> getEncounters(DataItemContext dataItemContext) {
    // Check if patient IDs are available for retrieving Encounter resources
    if (!patientIdsCouldBeFound(patientIds, ResourceType.Encounter)) {
      return new ArrayList<>();
    }

    // Initialize counters for logging and tracking
    AtomicLong countProcessedEncounter =
        new AtomicLong(0); // Tracks the number of processed sublists
    AtomicLong overallTotal = new AtomicLong(0); // Tracks the total number of expected encounters
    Set<Encounter> encounterSet =
        ConcurrentHashMap.newKeySet(); // Stores results without duplicates

    // Retrieve ICU-specific configuration for identifying ICU locations via service providers
    Set<String> icuLocationIdsServiceProvider =
        getGlobalConfiguration().getServiceProviderIdentifierOfIcuLocations();
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
    log.debug("Tried to retrieve {} encounter resources.", overallTotal.get());
    log.debug("Retrieved {} encounter resources.", encounterSet.size());

    // Warn if there is a mismatch between the expected and retrieved encounter counts
    if (overallTotal.get() != encounterSet.size()) {
      log.warn(
          "Mismatch between total and retrieved encounters. {} possibly missing due to batch size issues.",
          overallTotal.get() - encounterSet.size());
    }

    // Return the retrieved encounters as a list
    return new ArrayList<>(encounterSet);
  }

  private void processSublist(
      List<String> patientIdSublist,
      DataItemContext dataItemContext,
      Set<Encounter> encounterSet,
      AtomicLong overallTotal,
      Set<String> icuLocationIdsServiceProvider,
      boolean serviceProviderIdentifierFound) {
    // Retrieve the total count of encounters for the current patient sublist
    var totalBundle =
        this.getSearchService()
            .getInitialBundle(
                fhirServerQuerySuffixBuilder.getEncounters(
                    this, patientIdSublist, dataItemContext, true));
    overallTotal.addAndGet(totalBundle.getTotal());

    // Retrieve the first page of encounters for the current sublist
    var initialBundle =
        this.getSearchService()
            .getInitialBundle(
                fhirServerQuerySuffixBuilder.getEncounters(
                    this, patientIdSublist, dataItemContext, false));
    processEncounterBundle(
        initialBundle, encounterSet, icuLocationIdsServiceProvider, serviceProviderIdentifierFound);

    // Handle pagination for additional pages of encounter resources
    while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
      initialBundle = this.getSearchService().getBundlePart(initialBundle.getLink(NEXT).getUrl());
      processEncounterBundle(
          initialBundle,
          encounterSet,
          icuLocationIdsServiceProvider,
          serviceProviderIdentifierFound);
    }
  }

  private void processEncounterBundle(
      Bundle bundle,
      Set<Encounter> encounterSet,
      Set<String> icuLocationIdsServiceProvider,
      boolean serviceProviderIdentifierFound) {
    // Process each entry in the bundle
    bundle
        .getEntry()
        .forEach(
            bundleEntry -> {
              if (bundleEntry.getResource() instanceof Encounter encounter) {
                encounterSet.add(encounter); // Add the encounter to the result set
                processEncounterLocations(
                    encounter, icuLocationIdsServiceProvider, serviceProviderIdentifierFound);
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
  public List<Procedure> getProcedures() {

    // If no case ids could be found, no procedures need to be determined, because the evaluation
    // logic is based on data from the encounter resource.
    if (!encounterIdsCouldBeFound(encounterIds, ResourceType.Procedure)) {
      return new ArrayList<>();
    }

    // Logging progress
    AtomicLong countProcessedProcedures = new AtomicLong(0);

    // Initialization of the outgoing set
    Set<Procedure> setProcedures = ConcurrentHashMap.newKeySet();

    // Sum of all the encounter resources found
    AtomicLong overallTotal = new AtomicLong(0);

    // Input handling
    List<String> patientIdList = new ArrayList<>(patientIds);
    List<List<String>> patientIdSublists = splitList(patientIdList, this.getBatchSize());

    patientIdSublists.forEach(
        patientIdSublist -> {
          // Ask the total count to track if in the end the numbers are the same
          var totalBundle =
              this.getSearchService()
                  .getInitialBundle(
                      fhirServerQuerySuffixBuilder.getProcedures(this, patientIdSublist, true));
          var initialBundle =
              this.getSearchService()
                  .getInitialBundle(
                      fhirServerQuerySuffixBuilder.getProcedures(this, patientIdSublist, false));
          log.debug("Procedures found for this part bundle: {}", totalBundle.getTotal());
          overallTotal.addAndGet(totalBundle.getTotal());

          // Collecting the encounter resources
          initialBundle.getEntry().parallelStream()
              .forEach(
                  bundleEntry -> {
                    if (bundleEntry.getResource() instanceof Procedure procedure) {
                      setProcedures.add(procedure);
                    }
                  });
          // Pagination through follow pages if existing
          while (initialBundle.hasLink() && initialBundle.getLink(NEXT) != null) {
            initialBundle =
                this.getSearchService().getBundlePart(initialBundle.getLink(NEXT).getUrl());
            initialBundle
                .getEntry()
                .forEach(
                    bundleEntry -> {
                      if (bundleEntry.getResource() instanceof Procedure procedure) {
                        setProcedures.add(procedure);
                      }
                    });
          }
          logStatusDataRetrievalParallel(
              patientIdList.size(),
              countProcessedProcedures.getAndIncrement(),
              Enumerations.FHIRAllTypes.PROCEDURE.getDisplay());
        });
    return new ArrayList<>(setProcedures);
  }

  @Override
  public List<Procedure> getProcedures(
      List<UkbEncounter> listUkbEncounters,
      List<UkbLocation> listUkbLocations,
      List<UkbObservation> listUkbObservations,
      List<UkbCondition> listUkbConditions,
      DataItemContext dataItemContext) {
    return getProcedures();
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
          reqBundleEntry =
              this.getSearchService()
                  .getBundleData(
                      fhirServerQuerySuffixBuilder.getLocations(this, locationIdSubList));

          reqBundleEntry.parallelStream()
              .forEach(
                  bundleEntry -> {
                    if (bundleEntry.getResource() instanceof Location location) {
                      setLocations.add(location);
                    }
                  });
        });
    return new ArrayList<>(setLocations);
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
          "Retrieving {} data: {}/{}", resourceType, counter * this.getBatchSize(), totalEntries);
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
