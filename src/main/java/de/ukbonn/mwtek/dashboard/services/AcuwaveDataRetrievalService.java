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

import static de.ukbonn.mwtek.dashboard.enums.AcuwaveDataSourceType.CLAPP;
import static de.ukbonn.mwtek.dashboard.enums.AcuwaveDataSourceType.PDMS_REPORTING_DB;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_CONDITIONS;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_OBSERVATIONS_PCR;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_OBSERVATIONS_VARIANTS;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_PROCEDURES_ECMO;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.COVID_PROCEDURES_VENTILATION;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.INFLUENZA_CONDITIONS;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.INFLUENZA_OBSERVATIONS_PCR;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.ConfigurationContext.PREDICTION_MODEL_UKB_OBS_CODES;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.extractInputCodes;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.extractKidsRadarDiagnosisConditions;
import static de.ukbonn.mwtek.dashboard.misc.DateHelper.getCalendarYearsPeriod;
import static de.ukbonn.mwtek.dashboard.misc.LoggingHelper.logWarningForUnexpectedResource;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.encounterIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.locationIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.patientIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.KJP;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.RSV;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;
import static de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameterCodes.VALID_LOINC_CODES_HIS;
import static de.ukbonn.mwtek.dashboardlogic.tools.ObservationFilter.hasObservationLoincCode;
import static de.ukbonn.mwtek.utilities.generic.collections.ListTools.splitList;

import de.ukbonn.mwtek.dashboard.DashboardApplication;
import de.ukbonn.mwtek.dashboard.configuration.AcuwaveSearchConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.GlobalConfiguration;
import de.ukbonn.mwtek.dashboard.enums.AcuwaveDataSourceType;
import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import de.ukbonn.mwtek.dashboard.exceptions.SearchException;
import de.ukbonn.mwtek.dashboard.interfaces.DataSourceType;
import de.ukbonn.mwtek.dashboard.interfaces.SearchService;
import de.ukbonn.mwtek.dashboard.misc.AcuwaveQuerySuffixBuilder;
import de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer;
import de.ukbonn.mwtek.dashboard.misc.ResourceHandler;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.Covid;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.mapping.kdscase.valuesets.KdsEncounterFixedValues;
import de.ukbonn.mwtek.utilities.fhir.misc.ResourceConverter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import java.time.Year;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpServerErrorException;

/**
 * All methods to retrieve the data necessary for the corona dashboard from an Acuwave server.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
public class AcuwaveDataRetrievalService extends AbstractDataRetrievalService {

  /** all calendar month of a year, needed for sending parallelize requests */
  private final List<Integer> setMonths = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

  @Getter private final AcuwaveSearchConfiguration acuwaveSearchConfiguration;
  @Getter private final GlobalConfiguration globalConfiguration;
  @Getter @Setter private List<Integer> covidOrbisLabPcrCodes;
  @Getter @Setter private List<Integer> covidOrbisLabVariantCodes;

  @Getter @Setter private Set<Integer> predictionModelUkbObservationOrbisCreatinineCodes;

  @Getter @Setter private Set<Integer> predictionModelUkbObservationOrbisLactateCodes;

  @Getter @Setter private Map<String, Set<Integer>> predictionModelUkbObservationOrbisCodes;

  @Getter @Setter private List<Integer> influenzaOrbisLabPcrCodes;

  Logger logger = LoggerFactory.getLogger(DashboardApplication.class);

  public AcuwaveDataRetrievalService(
      SearchService searchService,
      AcuwaveSearchConfiguration acuwaveSearchConfiguration,
      GlobalConfiguration globalConfiguration) {
    this.acuwaveSearchConfiguration = acuwaveSearchConfiguration;
    this.globalConfiguration = globalConfiguration;
    initializeSearchService(searchService);
  }

  /**
   * Initialize everything related to the server searches (syntax codes, user configurations).
   *
   * @param searchService The service that performs the server search requests
   */
  private void initializeSearchService(SearchService searchService) {
    this.setSearchService(searchService);
    this.setCovidOrbisLabPcrCodes(acuwaveSearchConfiguration.getCovidOrbisLabPcrCodes());
    this.setCovidOrbisLabVariantCodes(acuwaveSearchConfiguration.getCovidOrbisLabVariantCodes());
    this.setCovidLabPcrCodes(extractInputCodes(globalConfiguration, COVID_OBSERVATIONS_PCR));
    this.setCovidLabVariantCodes(
        extractInputCodes(globalConfiguration, COVID_OBSERVATIONS_VARIANTS));
    this.setProcedureVentilationCodes(
        extractInputCodes(globalConfiguration, COVID_PROCEDURES_VENTILATION));
    this.setProcedureEcmoCodes(extractInputCodes(globalConfiguration, COVID_PROCEDURES_ECMO));
    // Reading of the icd codes from the configuration and transforming it into a list
    this.setCovidIcdCodes(extractInputCodes(globalConfiguration, COVID_CONDITIONS));
    // Prediction model setter
    this.setPredictionModelUkbObservationCodes(
        extractInputCodes(globalConfiguration, PREDICTION_MODEL_UKB_OBS_CODES));
    this.setPredictionModelUkbObservationOrbisCodes(
        readUkbObservationOrbisCodes(
            acuwaveSearchConfiguration.getPredictionModelUkbObservationOrbisCodes()));
    // Influenza data setter
    this.setInfluenzaOrbisLabPcrCodes(acuwaveSearchConfiguration.getInfluenzaOrbisLabPcrCodes());
    this.setInfluenzaIcdCodes(extractInputCodes(globalConfiguration, INFLUENZA_CONDITIONS));
    this.setInfluenzaLabPcrCodes(
        extractInputCodes(globalConfiguration, INFLUENZA_OBSERVATIONS_PCR));
    this.setKidsRadarKjpIcdCodes(extractKidsRadarDiagnosisConditions(globalConfiguration, KJP));
    this.setKidsRadarRsvIcdCodes(extractKidsRadarDiagnosisConditions(globalConfiguration, RSV));
  }

  private Map<String, Set<Integer>> readUkbObservationOrbisCodes(
      Map<String, String> predictionModelUkbObservationOrbisCodes) {
    Map<String, Set<Integer>> orbisCodesByFinding = new HashMap<>();
    predictionModelUkbObservationOrbisCodes.forEach(
        (name, list) -> {
          try {
            orbisCodesByFinding.put(
                name,
                StringUtils.commaDelimitedListToSet(list).stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet()));
          } catch (Exception ex) {
            logger.warn("Unable to parse the values from {} to Integer [{}]", name, list);
          }
        });

    return orbisCodesByFinding;
  }

  @Override
  public List<Observation> getObservations(DataItemContext dataItemContext) {

    Set<Observation> setObservations = ConcurrentHashMap.newKeySet();

    setMonths.parallelStream()
        .forEach(
            month -> {
              try {
                List<Bundle.BundleEntryComponent> listTemp =
                    this.getSearchService()
                        .getBundleData(
                            new AcuwaveQuerySuffixBuilder()
                                .getObservations(this, month, false,
                                    dataItemContext));
                listTemp.forEach(
                    bundleEntry -> {
                      if (bundleEntry.getResource().getResourceType() == ResourceType.Observation) {
                        Observation obs = (Observation) bundleEntry.getResource();
                        ResourceHandler.storeObservationPatientKeys(
                            obs, patientIds, encounterIds, this.getServerType());
                        setObservations.add(obs);
                      }
                    });
              } catch (HttpServerErrorException e) {
                throw new HttpServerErrorException(
                    e.getStatusCode(),
                    "Retrieval of the observation resources failed because of the following remote"
                        + " server error: "
                        + e.getMessage());
              } catch (Exception e) {
                logger.error("Retrieval of the observation resources failed. {}", month, e);
              }
            });
    return new ArrayList<>(setObservations);
  }

  @Override
  public List<Condition> getConditions(DataItemContext dataItemContext) {
    Set<Condition> setConditions = ConcurrentHashMap.newKeySet();

    setMonths.parallelStream()
        .forEach(
            month -> {
              try {
                List<Bundle.BundleEntryComponent> listTemp =
                    this.getSearchService()
                        .getBundleData(
                            new AcuwaveQuerySuffixBuilder()
                                .getConditions(this, month, false,
                                    dataItemContext));
                listTemp.forEach(
                    bundleEntry -> {
                      if (bundleEntry.getResource().getResourceType() == ResourceType.Condition) {
                        Condition cond = (Condition) bundleEntry.getResource();
                        ResourceHandler.storeConditionPatientKeys(
                            cond, patientIds, encounterIds, this.getServerType());
                        setConditions.add(cond);
                      }
                    });
              } catch (Exception e) {
                logger.error(
                    "Retrieval condition resources: Unable to build a json module chain: {}",
                    e.getMessage());
              }
            });
    return new ArrayList<>(setConditions);
  }

  @Override
  public List<Patient> getPatients(
      List<UkbObservation> ukbObservations,
      List<UkbCondition> ukbConditions,
      DataItemContext dataItemContext) {

    // A query is only useful if at least one patient id is specified.
    if (!patientIdsCouldBeFound(patientIds, ResourceType.Patient)) {
      return new ArrayList<>();
    }

    Set<Patient> patientsOutput = ConcurrentHashMap.newKeySet();
    // Since the patient (and thus also the encounter) resources are just relevant for statistics
    // of SARS-CoV-2 patient it reduces the amount of encounter by a lot if its getting
    // prefiltered before
    switch (dataItemContext) {
      case COVID ->
          patientIds =
              handleFilterPatientRetrieval(
                  dataItemContext,
                  this.acuwaveSearchConfiguration.getFilterPatientRetrieval(),
                  ukbObservations,
                  ukbConditions);
      case INFLUENZA ->
          patientIds =
              handleFilterPatientRetrieval(
                  dataItemContext,
                  this.acuwaveSearchConfiguration.getInfluenzaFilterPatientRetrieval(),
                  ukbObservations,
                  ukbConditions);
    }

    // Splitting the entire list into smaller lists to parallelize requests
    List<List<String>> patientIdSubLists =
        splitList(new ArrayList<>(patientIds), this.getBatchSize());

    patientIdSubLists.parallelStream()
        .forEach(
            patientIdList -> {
              try {
                List<Bundle.BundleEntryComponent> listTemp =
                    this.getSearchService()
                        .getBundleData(
                            new AcuwaveQuerySuffixBuilder().getPatients(this, patientIdList));
                listTemp.forEach(
                    bundleEntry -> {
                      if (bundleEntry.getResource().getResourceType() == ResourceType.Patient) {
                        patientsOutput.add((Patient) bundleEntry.getResource());
                      }
                    });

              } catch (Exception e) {
                logger.error(
                    "Retrieval patient resources: Unable to build a json module chain: "
                        + e.getMessage());
              }
            });
    return new ArrayList<>(patientsOutput);
  }

  @Override
  public List<Encounter> getEncounters(DataItemContext dataItemContext) {
    Set<Encounter> setEncounters = ConcurrentHashMap.newKeySet();

    // A query is only useful if at least one patient id is specified.
    if (!patientIdsCouldBeFound(patientIds, ResourceType.Encounter)) {
      return new ArrayList<>();
    }

    // Splitting the entire list into smaller lists to parallelize requests
    List<List<String>> patientIdSubLists =
        splitList(new ArrayList<>(patientIds), this.getBatchSize());

    patientIdSubLists.parallelStream()
        .forEach(
            patientIdList -> {
              try {
                List<Bundle.BundleEntryComponent> listTemp =
                    this.getSearchService()
                        .getBundleData(
                            new AcuwaveQuerySuffixBuilder()
                                .getEncounters(this, patientIdList, dataItemContext, false));
                listTemp.forEach(
                    bundleEntry -> {
                      if (bundleEntry.getResource().getResourceType() == ResourceType.Encounter) {
                        Encounter encounter = (Encounter) bundleEntry.getResource();
                        encounter
                            .getLocation()
                            .forEach(
                                loc ->
                                    locationIds.add(
                                        loc.getLocation().getReference().split("/")[1]));
                        setEncounters.add(encounter);
                      }
                    });

              } catch (NullPointerException e) {
                logger.warn(
                    "Retrieval encounter resources:: Issue in the location retrieval (maybe the id"
                        + " of a location resource is null): {}",
                    e.getMessage());
              } catch (Exception e) {
                logger.error(
                    "Retrieval encounter resources: Unable to build a json module chain: {}",
                    e.getMessage());
              }
            });

    return new ArrayList<>(setEncounters);
  }

  @Override
  public List<Procedure> getProcedures() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Procedure> getProcedures(
      List<UkbEncounter> listUkbEncounters,
      List<UkbLocation> listUkbLocations,
      List<UkbObservation> listUkbObservations,
      List<UkbCondition> listUkbConditions,
      DataItemContext dataItemContext) {

    // If no case ids could be found, no procedures need to be determined, because the evaluation
    // logic is based on data from the encounter resource.
    if (!patientIdsCouldBeFound(encounterIds, ResourceType.Procedure)) {
      return new ArrayList<>();
    }

    Set<Procedure> setProcedures = ConcurrentHashMap.newKeySet();

    // The finally used encounter id list which can differ from the original encounter id list
    // due to filtering steps
    Set<String> encounterIdsInput;
    // Filter the encounter ids, to those of Encounters with at least 1 inpatient or ICU (under
    // review) stay.
    // Requires: encounter + maybe location resources
    if (acuwaveSearchConfiguration.getFilterProcedureRetrieval()) {
      // determine all the icu locations
      Set<String> listIcuIds =
          listUkbLocations.parallelStream()
              .filter(Location::hasType)
              .filter(x -> x.getType().get(0).hasCoding())
              .filter(x -> x.getType().get(0).getCoding().get(0).getCode().equals(ICU.getValue()))
              .map(UkbLocation::getId)
              .collect(Collectors.toSet());

      // add the orbis internal ids of non-ICU wards to the list if requested
      if (!acuwaveSearchConfiguration.getFilterProcedureRetrievalAdditionalWards().isEmpty()) {
        listIcuIds.addAll(acuwaveSearchConfiguration.getFilterProcedureRetrievalAdditionalWards());
      }

      // figure out if encounter is inpatient
      List<UkbEncounter> listEncountersInpatient =
          listUkbEncounters.parallelStream()
              .filter(Encounter::hasClass_)
              .filter(x -> x.getClass_().hasCode())
              .filter(
                  x ->
                      KdsEncounterFixedValues.ENCOUNTER_CLASS_INPATIENT_CODES.contains(
                          x.getClass_().getCode()))
              .toList();

      // get all the inpatient encounter ids with at least one icu transfer
      Set<String> listPatientsIcu = ConcurrentHashMap.newKeySet();
      listEncountersInpatient.parallelStream()
          .forEach(
              enc -> {
                // Check all entries in the location attribute in the Encounter resource for ICU
                // relevance.
                boolean encWithIcu =
                    enc.getLocation().stream()
                        .anyMatch(
                            loc ->
                                listIcuIds.contains(
                                    loc.getLocation().getReference().split("/")[1]));
                if (encWithIcu) {
                  listPatientsIcu.add(enc.getPatientId());
                }
              });

      // Get all icu case ids where the patient got at least one positive SARS-CoV-2 lab finding
      Set<String> patientIdsIcuPositiveFindings =
          CoronaResultFunctionality.getPidsWithPosLabResult(
                  listUkbObservations,
                  ConfigurationTransformer.extractInputCodeSettings(this),
                  dataItemContext, ConfigurationTransformer.extractQualitativeLabCodesSettings(this))
              .parallelStream()
              .filter(listPatientsIcu::contains)
              .collect(Collectors.toSet());
      logger.debug(
          "Number of patients with ICU and positive SARS-CoV2 PCR test finding: {}",
          patientIdsIcuPositiveFindings.size());

      // get all cases with at least 1 icu transfer AND an U07.1 diagnosis and add
      // them
      // to the set with the positive lab findings
      patientIdsIcuPositiveFindings.addAll(
          CoronaResultFunctionality.getPidsWithGivenIcdCodes(
                  listUkbConditions,
                  ConfigurationTransformer.extractInputCodeSettings(this),
                  dataItemContext)
              .parallelStream()
              .filter(listPatientsIcu::contains)
              .collect(Collectors.toSet()));

      logger.debug(
          "Previous list extended by the patients with SARS-CoV2 conditions: {}",
          patientIdsIcuPositiveFindings.size());

      // now get all the inpatient cases of the patients that fulfilled the criteria
      encounterIdsInput =
          listEncountersInpatient.parallelStream()
              .filter(x -> patientIdsIcuPositiveFindings.contains(x.getPatientId()))
              .map(UkbEncounter::getCaseId)
              .collect(Collectors.toSet());
    } else
    // use total list then
    {
      encounterIdsInput = encounterIds;
    }

    List<List<String>> encounterIdSubLists =
        splitList(new ArrayList<>(encounterIdsInput), this.getBatchSize());
    // A query is only useful if at least one encounter id is specified.
    if (!encounterIds.isEmpty()) {
      encounterIdSubLists.parallelStream()
          .forEach(
              encounterIds -> {
                try {
                  List<Bundle.BundleEntryComponent> listTemp =
                      this.getSearchService()
                          .getBundleData(
                              new AcuwaveQuerySuffixBuilder()
                                  .getProcedures(this, encounterIds, null));
                  listTemp.forEach(
                      bundleEntry -> {
                        if (bundleEntry.getResource().getResourceType() == ResourceType.Procedure) {
                          Procedure procedure = (Procedure) bundleEntry.getResource();
                          setProcedures.add(procedure);
                        }
                      });
                } catch (Exception e) {
                  logger.error(
                      "Retrieval procedure resources: Unable to build a json module chain: {}",
                      e.getMessage());
                }
              });
    } else {
      logger.error(
          "Unable to retrieve procedures resources since no encounter ids could be determined via"
              + " input filters (Observation + Condition).");
    }

    return new ArrayList<>(setProcedures);
  }

  @Override
  public List<Location> getLocations() {

    // If no case ids could be found, no procedures need to be determined, because the evaluation
    // logic is based on data from the encounter resource.
    if (!locationIdsCouldBeFound(locationIds, ResourceType.Location)) {
      return new ArrayList<>();
    }

    Set<Location> setLocations = ConcurrentHashMap.newKeySet();
    // the location module requires location id as number
    List<Integer> locationIdsNumber =
        locationIds.stream().map(Integer::parseInt).collect(Collectors.toList());
    List<List<Integer>> locationIdSublists = splitList(locationIdsNumber, getBatchSize());

    locationIdSublists.parallelStream()
        .forEach(
            locationIdSublist -> {
              try {
                List<Bundle.BundleEntryComponent> listTemp =
                    this.getSearchService()
                        .getBundleData(
                            new AcuwaveQuerySuffixBuilder().getLocations(this, locationIdSublist));
                listTemp.forEach(
                    bundleEntry -> {
                      if (bundleEntry.getResource().getResourceType() == ResourceType.Location) {
                        setLocations.add((Location) bundleEntry.getResource());
                      }
                    });
              } catch (Exception e) {
                logger.error(
                    "Retrieval location resources: Unable to build a json module chain: {}",
                    e.getMessage());
              }
            });
    return new ArrayList<>(setLocations);
  }

  public final ServerTypeEnum getServerType() {
    return ServerTypeEnum.ACUWAVE;
  }

  @Override
  public int getBatchSize() {
    return this.acuwaveSearchConfiguration.getBatchSize();
  }

  @Override
  public List<Encounter> getIcuEncounters() throws SearchException {
    Set<Encounter> encounters = ConcurrentHashMap.newKeySet();

    try {
      // If case ids are set in the settings file the search will run case by case, not periodically
      List<String> caseIdsSettings = acuwaveSearchConfiguration.getPredictionModelUkbCaseIds();
      if (caseIdsSettings.isEmpty()) {
        // Parallelize the queries in calendar years to speed it up and to get rid of memory
        // issues due to large bulks
        List<Integer> calendarYears =
            getCalendarYearsPeriod(Covid.QUALIFYING_YEAR, Year.now().getValue());
        // If an entry in the settings file forces querying over certain years this will
        // overwrite the default.
        if (acuwaveSearchConfiguration.getPredictionModelYears() != null) {
          calendarYears = acuwaveSearchConfiguration.getPredictionModelYears();
        }
        calendarYears.parallelStream()
            .forEach(
                year -> {
                  List<Bundle.BundleEntryComponent> listTemp =
                      this.getSearchService()
                          .getBundleData(
                              new AcuwaveQuerySuffixBuilder().getIcuEncounters(this, year));
                  listTemp.parallelStream()
                      .forEach(
                          bundleEntry -> {
                            if (bundleEntry.getResource().getResourceType()
                                == ResourceType.Encounter) {
                              encounters.add((Encounter) bundleEntry.getResource());
                            }
                          });
                });
      } else {
        List<Bundle.BundleEntryComponent> listTemp =
            this.getSearchService()
                .getBundleData(
                    new AcuwaveQuerySuffixBuilder().getEncountersDebug(this, caseIdsSettings));
        listTemp.parallelStream()
            .forEach(
                bundleEntry -> {
                  if (bundleEntry.getResource().getResourceType() == ResourceType.Encounter) {
                    encounters.add((Encounter) bundleEntry.getResource());
                  }
                });
      }

    } catch (Exception e) {
      // Rethrow the exception as a more specific exception
      throw new SearchException("Error occurred while searching for ICU encounters", e);
    }

    return new ArrayList<>(encounters);
  }

  /**
   * Returns a list of ICU episodes for the specified case IDs.
   *
   * @param caseIds A collection of case ids that are used to query data from the acuwave server.
   * @return A list of ICU episodes
   */
  @Override
  public List<CoreBaseDataItem> getIcuEpisodes(Collection<String> caseIds) throws SearchException {
    // Create thread-safe collections to store the results
    Set<EpisodeOfCare> episodeOfCares = ConcurrentHashMap.newKeySet();
    Set<CoreBaseDataItem> coreBaseDataItems = ConcurrentHashMap.newKeySet();
    Map<String, String> episodeOfCareEncounterMap = new ConcurrentHashMap<>();

    try {
      // Split the case ids into smaller lists to parallelize requests
      List<List<String>> caseIdSubLists = splitList(new ArrayList<>(caseIds), this.getBatchSize());

      caseIdSubLists.parallelStream()
          .forEach(
              cases -> {
                List<Bundle.BundleEntryComponent> bundleEntries =
                    this.getSearchService()
                        .getBundleData(new AcuwaveQuerySuffixBuilder().getIcuEpisodes(this, cases));
                // Process each bundle entry
                bundleEntries.parallelStream()
                    .forEach(
                        bundleEntry -> {
                          if (bundleEntry.getResource() instanceof EpisodeOfCare episodeOfCare) {
                            episodeOfCares.add(episodeOfCare);
                          } else if (bundleEntry.getResource() instanceof Encounter encounter) {
                            if (encounter.hasEpisodeOfCare()) {
                              // we need to convert the encounter resource to be able to use the
                              // "getCaseId"
                              // method
                              UkbEncounter ukbEncounter =
                                  (UkbEncounter) ResourceConverter.convert(encounter);
                              encounter
                                  .getEpisodeOfCare()
                                  .forEach(
                                      encEpisodeRef ->
                                          episodeOfCareEncounterMap.put(
                                              encEpisodeRef.getResource().getIdElement().getValue(),
                                              ukbEncounter.getCaseId()));
                            }
                          }
                        });
              });
    } catch (Exception e) {
      // Rethrow the exception as a more specific exception
      throw new SearchException("Error occurred while searching for ICU episodes", e);
    }

    // Create CoreBaseDataItems from the mapping entries
    episodeOfCareEncounterMap.forEach(
        (x, y) -> {
          // Find the corresponding episode resource
          Optional<EpisodeOfCare> currentEpisodeOfCare =
              episodeOfCares.stream().filter(episode -> episode.getId().equals(x)).findFirst();
          currentEpisodeOfCare.ifPresent(
              episodeOfCare ->
                  coreBaseDataItems.add(
                      new CoreBaseDataItem(
                          y,
                          x,
                          null,
                          episodeOfCare.getPeriod().getStart(),
                          episodeOfCare.getPeriod().getEnd(),
                          null)));
        });

    return new ArrayList<>(coreBaseDataItems);
  }

  @Override
  public List<CoreBaseDataItem> getUkbRenalReplacementObservations(
      Collection<String> encounterIds, Set<Integer> orbisCodes) {
    //    Set<Observation> observations = ConcurrentHashMap.newKeySet();
    Set<CoreBaseDataItem> CoreBaseDataItems = ConcurrentHashMap.newKeySet();

    // A query is only useful if at least one patient id is specified.
    if (!encounterIdsCouldBeFound(encounterIds, ResourceType.Encounter)) {
      return new ArrayList<>();
    }

    // Splitting the entire list into smaller lists to parallelize requests
    List<List<String>> encounterIdSubLists =
        splitList(new ArrayList<>(encounterIds), this.getBatchSize());

    encounterIdSubLists.parallelStream()
        .forEach(
            encounterIdSubList -> {
              try {
                List<Bundle.BundleEntryComponent> listTemp =
                    this.getSearchService()
                        .getBundleData(
                            new AcuwaveQuerySuffixBuilder()
                                .getUkbRenalReplacementObservations(
                                    this, encounterIdSubList, orbisCodes));
                listTemp.forEach(
                    bundleEntry -> {
                      if (bundleEntry.getResource().getResourceType() == ResourceType.Observation) {
                        Observation observation = (Observation) bundleEntry.getResource();
                        if (hasObservationLoincCode(observation, VALID_LOINC_CODES_HIS)) {
                          if (observation.hasValueQuantity()
                              && observation.hasEffectiveDateTimeType()) {
                            CoreBaseDataItems.add(
                                new CoreBaseDataItem(
                                    observation.getEncounter().getIdentifier().getValue(),
                                    observation.getEncounter().getReference(),
                                    observation.getValueQuantity().getValue().doubleValue(),
                                    observation.getEffectiveDateTimeType().getValue(),
                                    null,
                                    observation.getId()));
                          }
                        }
                        // observations.add(observation);
                      }
                    });
              } catch (Exception e) {
                logger.error(
                    "Error while getting and processing the renal replacement resources: "
                        + e.getMessage());
              }
            });

    return new ArrayList<>(CoreBaseDataItems);
  }

  @Override
  public List<CoreBaseDataItem> getUkbRenalReplacementBodyWeight(
      Collection<String> encounterIds, DataSourceType dataSourceType) {
    // To keep it thread-safe we will use a concurrent map to store caseid and the first found
    // data item
    Map<String, CoreBaseDataItem> caseIdItemMap = new ConcurrentHashMap<>();

    // A query is only useful if at least one patient id is specified.
    if (!encounterIdsCouldBeFound(encounterIds, ResourceType.Encounter)) {
      return new ArrayList<>();
    }

    // Split the entire list of encounter ids into smaller lists to parallelize requests
    List<List<String>> encounterIdSubLists =
        splitList(new ArrayList<>(encounterIds), this.getBatchSize());

    encounterIdSubLists.parallelStream()
        .forEach(
            encounterIdSublist -> {
              try {
                List<Bundle.BundleEntryComponent> listTemp = new ArrayList<>();
                // The output of both services should be the same fhir resources.
                switch ((AcuwaveDataSourceType) dataSourceType) {
                  case CLAPP ->
                      listTemp =
                          this.getSearchService()
                              .getBundleData(
                                  new AcuwaveQuerySuffixBuilder()
                                      .getUkbRenalReplacementBodyWeight(
                                          this, encounterIdSublist, CLAPP));
                  case PDMS_REPORTING_DB ->
                      listTemp =
                          this.getSearchService()
                              .getBundleData(
                                  new AcuwaveQuerySuffixBuilder()
                                      .getUkbRenalReplacementBodyWeight(
                                          this, encounterIdSublist, PDMS_REPORTING_DB));
                }

                // Casting the found observations to a list
                List<Observation> observations =
                    listTemp.stream()
                        .map(BundleEntryComponent::getResource)
                        .filter(resource -> resource.getResourceType() == ResourceType.Observation)
                        .map(Observation.class::cast) // Casting using method reference
                        .toList();

                // Create a map grouping observations by encounter identifier and keeping only the
                // oldest for
                // each encounter identifier
                Map<String, Observation> oldestBodyWeightMap =
                    observations.stream()
                        .collect(
                            Collectors.toMap(
                                observation ->
                                    observation.getEncounter().getIdentifier().getValue(),
                                observation -> observation,
                                (existing, replacement) -> {
                                  // Compare LocalDateTime using compareTo and choose the older
                                  // observation
                                  if (existing
                                          .getEffectiveDateTimeType()
                                          .getValue()
                                          .compareTo(
                                              replacement.getEffectiveDateTimeType().getValue())
                                      < 0) {
                                    return existing;
                                  } else {
                                    return replacement;
                                  }
                                }));

                oldestBodyWeightMap.forEach(
                    (caseId, bodyWeightObservation) -> {
                      // regarding FE its fine to ignore implausible values (>300kg) and children
                      // (<30kg)
                      double bodyWeight =
                          bodyWeightObservation.getValueQuantity().getValue().doubleValue();
                      if (isValidBodyWeight(bodyWeight)) {
                        caseIdItemMap.put(
                            caseId,
                            new CoreBaseDataItem(
                                caseId,
                                bodyWeightObservation.getEncounter().getReference(),
                                bodyWeightObservation.getValueQuantity().getValue().doubleValue(),
                                null,
                                null,
                                bodyWeightObservation.getId()));
                      } else {
                        logger.trace(
                            "BodyWeight: {} got filtered. Case:{}",
                            bodyWeight,
                            bodyWeightObservation.getEncounter().getReference());
                      }
                    });
              } catch (Exception e) {
                logger.error(
                    "Error while getting and processing the body weight resources: "
                        + e.getMessage());
              }
            });

    return new ArrayList<>(caseIdItemMap.values());
  }

  /**
   * Regarding FE its fine to ignore implausible values (>300kg) and children (<30kg)
   *
   * @param bodyWeight Body weight in kg.
   */
  private static boolean isValidBodyWeight(double bodyWeight) {
    return bodyWeight > 30 && bodyWeight < 300;
  }

  @Override
  public List<CoreBaseDataItem> getUkbRenalReplacementUrineOutput(
      Collection<String> encounterIds, DataSourceType dataSourceType) {
    //    Set<Observation> observations = ConcurrentHashMap.newKeySet();
    Set<CoreBaseDataItem> coreBaseDataItems = ConcurrentHashMap.newKeySet();

    // A query is only useful if at least one patient id is specified.
    if (!encounterIdsCouldBeFound(encounterIds, ResourceType.Encounter)) {
      return new ArrayList<>();
    }

    // Splitting the entire list into smaller lists to parallelize requests
    List<List<String>> encounterIdSubLists =
        splitList(new ArrayList<>(encounterIds), this.getBatchSize());

    // Map with the case id and the (forced to be unique) timestamp of the observation entry
    ConcurrentMap<Entry<String, Date>, CoreBaseDataItem> map = new ConcurrentHashMap<>();

    encounterIdSubLists.parallelStream()
        .forEach(
            encounterIdSublist -> {
              try {
                List<Bundle.BundleEntryComponent> listTemp = new ArrayList<>();
                switch ((AcuwaveDataSourceType) dataSourceType) {
                  case CLAPP ->
                      listTemp =
                          this.getSearchService()
                              .getBundleData(
                                  new AcuwaveQuerySuffixBuilder()
                                      .getUkbRenalReplacementUrineOutput(
                                          this, encounterIdSublist, CLAPP));
                  case PDMS_REPORTING_DB ->
                      listTemp =
                          this.getSearchService()
                              .getBundleData(
                                  new AcuwaveQuerySuffixBuilder()
                                      .getUkbRenalReplacementUrineOutput(
                                          this, encounterIdSublist, PDMS_REPORTING_DB));
                }
                listTemp.forEach(
                    bundleEntry -> {
                      if (bundleEntry.getResource().getResourceType() == ResourceType.Observation) {
                        Observation observation = (Observation) bundleEntry.getResource();
                        if (observation.hasValueQuantity()) {
                          // It is possible that two urine output values got the same timestamp.
                          // If this is the case we try to sum these values up.
                          String encounterIdentifier =
                              observation.getEncounter().getIdentifier().getValue();
                          Date effectiveDateTime =
                              observation.getEffectiveDateTimeType().getValue();
                          Double value = observation.getValueQuantity().getValue().doubleValue();

                          // The key contains of a combination of encounter id and timestamp of the
                          // value
                          Map.Entry<String, Date> key =
                              new AbstractMap.SimpleEntry<>(encounterIdentifier, effectiveDateTime);

                          // If value already exists in the map, sum the values
                          map.compute(
                              key,
                              (k, existingItem) -> {
                                if (existingItem == null) {
                                  // Create a new item
                                  return new CoreBaseDataItem(
                                      encounterIdentifier,
                                      observation.getEncounter().getReference(),
                                      value,
                                      effectiveDateTime,
                                      null,
                                      observation.getId());
                                } else {
                                  // Create a new item with updated value
                                  return new CoreBaseDataItem(
                                      encounterIdentifier,
                                      existingItem.episodeId(),
                                      existingItem.value() + value,
                                      effectiveDateTime,
                                      null,
                                      existingItem.debugKey());
                                }
                              });
                        }
                      } else {
                        logWarningForUnexpectedResource(Observation.class, bundleEntry);
                      }
                    });
              } catch (Exception e) {
                logger.error(
                    "Error while getting and processing the urine output resources: {}",
                    e.getMessage());
              }
            });

    // add the items from the map to coreBaseDataItems
    coreBaseDataItems.addAll(map.values());

    return new ArrayList<>(coreBaseDataItems);
  }

  @Override
  public List<CoreBaseDataItem> getUkbRenalReplacementStart(
      Collection<String> encounterIds, DataSourceType dataSourceType) {
    // Set<Procedure> procedures = ConcurrentHashMap.newKeySet();
    Set<CoreBaseDataItem> coreBaseDataItems = ConcurrentHashMap.newKeySet();

    // A query is only useful if at least one patient id is specified.
    if (!encounterIdsCouldBeFound(encounterIds, ResourceType.Encounter)) {
      return new ArrayList<>();
    }

    // Splitting the entire list into smaller lists to parallelize requests
    List<List<String>> encounterIdSublists =
        splitList(new ArrayList<>(encounterIds), this.getBatchSize());

    encounterIdSublists.parallelStream()
        .forEach(
            encounterIdSublist -> {
              try {
                List<Bundle.BundleEntryComponent> listTemp = new ArrayList<>();
                switch ((AcuwaveDataSourceType) dataSourceType) {
                  case CLAPP ->
                      listTemp =
                          this.getSearchService()
                              .getBundleData(
                                  new AcuwaveQuerySuffixBuilder()
                                      .getUkbRenalReplacementStart(
                                          this, encounterIdSublist, CLAPP));
                  case PDMS_REPORTING_DB ->
                      listTemp =
                          this.getSearchService()
                              .getBundleData(
                                  new AcuwaveQuerySuffixBuilder()
                                      .getUkbRenalReplacementStart(
                                          this, encounterIdSublist, PDMS_REPORTING_DB));
                }
                listTemp.forEach(
                    bundleEntry -> {
                      if (bundleEntry.getResource().getResourceType() == ResourceType.Procedure) {
                        Procedure procedure = (Procedure) bundleEntry.getResource();
                        coreBaseDataItems.add(
                            new CoreBaseDataItem(
                                procedure.getEncounter().getIdentifier().getValue(),
                                procedure.getEncounter().getReference(),
                                null,
                                procedure.getPerformedPeriod().getStart(),
                                procedure.getPerformedPeriod().getEnd(),
                                procedure.getId()));
                      } else {
                        logWarningForUnexpectedResource(Observation.class, bundleEntry);
                      }
                    });
              } catch (Exception e) {
                logger.error(
                    "Error while getting and processing the renal replacement resources: "
                        + e.getMessage());
              }
            });
    //    renalReplacementModelParameterSetMap.put(START_REPLACEMENT, coreBaseDataItems);
    return new ArrayList<>(coreBaseDataItems);
  }
}
