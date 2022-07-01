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

import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.locationIdsCouldBeFound;
import static de.ukbonn.mwtek.dashboard.misc.ProcessHelper.patientIdsCouldBeFound;

import de.ukbonn.mwtek.dashboard.CoronaDashboardApplication;
import de.ukbonn.mwtek.dashboard.configuration.AcuwaveSearchConfiguration;
import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import de.ukbonn.mwtek.dashboard.interfaces.SearchService;
import de.ukbonn.mwtek.dashboard.misc.AcuwaveQuerySuffixBuilder;
import de.ukbonn.mwtek.dashboard.misc.ListHelper;
import de.ukbonn.mwtek.dashboard.misc.ResourceHandler;
import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All methods to retrieve the data necessary for the corona dashboard from an Acuwave server.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
public class AcuwaveDataRetrievalService extends AbstractDataRetrievalService {

  /**
   * all calendar month of a year, needed for sending parallelize requests
   */
  private final List<Integer> setMonths = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
  private final AcuwaveSearchConfiguration acuwaveSearchConfiguration;
  Logger logger = LoggerFactory.getLogger(CoronaDashboardApplication.class);

  public AcuwaveDataRetrievalService(SearchService searchService,
      AcuwaveSearchConfiguration acuwaveSearchConfiguration) {
    this.acuwaveSearchConfiguration = acuwaveSearchConfiguration;
    initializeSearchService(searchService);
  }

  /**
   * Initialize everything related to the server searches (syntax codes, user configurations).
   *
   * @param searchService The service that performs the server search requests
   */
  private void initializeSearchService(SearchService searchService) {
    this.setSearchService(searchService);
    this.setLabCodes(acuwaveSearchConfiguration.getOrbisLabCodes());
    this.setIcdCodes(acuwaveSearchConfiguration.getIcdCodes());
  }

  @Override
  public List<Observation> getObservations() {

    Set<Observation> setObservations = ConcurrentHashMap.newKeySet();

    setMonths.parallelStream().forEach(month -> {
      try {
        List<Bundle.BundleEntryComponent> listTemp = this.getSearchService()
            .getBundleData(new AcuwaveQuerySuffixBuilder().getObservations(this, month));
        listTemp.forEach(bundleEntry -> {
          if (bundleEntry.getResource().getResourceType() == ResourceType.Observation) {
            Observation obs = (Observation) bundleEntry.getResource();
            ResourceHandler.storeObservationPatientKeys(obs, patientIds, encounterIds,
                this.getServerType());
            setObservations.add(obs);
          }
        });
      } catch (Exception e) {
        logger.error("Retrieval observation resources: " + e.getMessage());
      }
    });

    return new ArrayList<>(setObservations);
  }

  @Override
  public List<Condition> getConditions() {
    Set<Condition> setConditions = ConcurrentHashMap.newKeySet();

    setMonths.parallelStream().forEach(month -> {
      try {
        List<Bundle.BundleEntryComponent> listTemp = this.getSearchService()
            .getBundleData(new AcuwaveQuerySuffixBuilder().getConditions(this, month));
        listTemp.forEach(bundleEntry -> {
          if (bundleEntry.getResource().getResourceType() == ResourceType.Condition) {
            Condition cond = (Condition) bundleEntry.getResource();
            ResourceHandler.storeConditionPatientKeys(cond, patientIds, encounterIds,
                this.getServerType());
            setConditions.add(cond);
          }
        });
      } catch (Exception e) {
        logger.error(
            "Retrieval condition resources: Unable to build a json module chain: "
                + e.getMessage());
      }
    });
    return new ArrayList<>(setConditions);
  }

  @Override
  public List<Patient> getPatients(List<UkbObservation> listUkbObservations,
      List<UkbCondition> listUkbConditions) {

    // A query is only useful if at least one patient id is specified.
    if (!patientIdsCouldBeFound(patientIds, ResourceType.Patient)) {
      return new ArrayList<>();
    }

    Set<Patient> setPatientsOutput = ConcurrentHashMap.newKeySet();
    // Since the patient (and thus also the encounter) resources are just relevant for statistics of SARS-CoV-2 patient it reduces the amount of encounter by a lot if its getting prefiltered before
    if (this.acuwaveSearchConfiguration.getFilterPatientRetrieval())
    // The global patient ID list is overwritten by exclusively SARS-CoV2-positive patients
    {
      patientIds =
          CoronaResultFunctionality.getPidsPosFinding(listUkbObservations, listUkbConditions);
    }

    // Splitting the entire list into smaller lists to parallelize requests
    List<List<String>> patientIdSublists =
        ListHelper.splitList(new ArrayList<>(patientIds), this.getBatchSize());

    patientIdSublists.parallelStream().forEach(patientIdList -> {
      try {
        List<Bundle.BundleEntryComponent> listTemp = this.getSearchService()
            .getBundleData(new AcuwaveQuerySuffixBuilder().getPatients(this, patientIdList));
        listTemp.forEach(bundleEntry -> {
          if (bundleEntry.getResource().getResourceType() == ResourceType.Patient) {
            setPatientsOutput.add((Patient) bundleEntry.getResource());
          }
        });

      } catch (Exception e) {
        logger.error(
            "Retrieval patient resources: Unable to build a json module chain: " + e.getMessage());
      }
    });
    return new ArrayList<>(setPatientsOutput);
  }

  @Override
  public List<Encounter> getEncounters() {
    Set<Encounter> setEncounters = ConcurrentHashMap.newKeySet();

    // A query is only useful if at least one patient id is specified.
    if (!patientIdsCouldBeFound(patientIds, ResourceType.Encounter)) {
      return new ArrayList<>();
    }

    // Splitting the entire list into smaller lists to parallelize requests
    List<List<String>> patientIdSublists =
        ListHelper.splitList(new ArrayList<>(patientIds), this.getBatchSize());

    patientIdSublists.parallelStream().forEach(patientIdList -> {
      try {
        List<Bundle.BundleEntryComponent> listTemp = this.getSearchService()
            .getBundleData(new AcuwaveQuerySuffixBuilder().getEncounters(this, patientIdList));
        listTemp.forEach(bundleEntry -> {
          if (bundleEntry.getResource().getResourceType() == ResourceType.Encounter) {
            Encounter encounter = (Encounter) bundleEntry.getResource();
            encounter.getLocation().forEach(
                loc -> locationIds.add(loc.getLocation().getReference().split("/")[1]));
            setEncounters.add(encounter);
          }
        });

      } catch (NullPointerException e) {
        logger.warn(
            "Retrieval encounter resources:: Issue in the location retrieval (maybe the id of a location resource is null): "
                + e.getMessage());
      } catch (Exception e) {
        logger.error(
            "Retrieval encounter resources: Unable to build a json module chain: "
                + e.getMessage());
      }
    });

    return new ArrayList<>(setEncounters);
  }


  @Override
  public List<Procedure> getProcedures() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Procedure> getProcedures(List<UkbEncounter> listUkbEncounters,
      List<UkbLocation> listUkbLocations, List<UkbObservation> listUkbObservations,
      List<UkbCondition> listUkbConditions) {

    // If no case ids could be found, no procedures need to be determined, because the evaluation logic is based on data from the encounter resource.
    if (!patientIdsCouldBeFound(encounterIds, ResourceType.Procedure)) {
      return new ArrayList<>();
    }

    Set<Procedure> setProcedures = ConcurrentHashMap.newKeySet();

    // The finally used encounter id list which can differ from the original encounter id list due to filtering steps
    Set<String> encounterIdsInput;
    // Filter the encounter ids, to those of Encounters with at least 1 inpatient or ICU (under review) stay.
    // Requires: encounter + maybe location resources
    if (acuwaveSearchConfiguration.getFilterProcedureRetrieval()) {
      // determine all the icu locations
      Set<String> listIcuIds = listUkbLocations.parallelStream().filter(Location::hasType)
          .filter(x -> x.getType().get(0).hasCoding())
          .filter(x -> x.getType().get(0).getCoding().get(0).getCode()
              .equals(CoronaFixedValues.ICU.getValue())).map(UkbLocation::getId)
          .collect(Collectors.toSet());

      // add the orbis internal ids of non-ICU wards to the list if requested
      if (acuwaveSearchConfiguration.getFilterProcedureRetrievalAdditionalWards().size() > 0) {
        listIcuIds.addAll(acuwaveSearchConfiguration.getFilterProcedureRetrievalAdditionalWards());
      }

      // figure out if encounter is inpatient
      List<UkbEncounter> listEncountersInpatient =
          listUkbEncounters.parallelStream().filter(Encounter::hasClass_)
              .filter(x -> x.getClass_().hasCode()).filter(x -> x.getClass_().getCode()
                  .equals(CoronaFixedValues.CASECLASS_INPATIENT.getValue())).toList();

      // get all the inpatient encounter ids with at least one icu transfer
      Set<String> listPatientsIcu = ConcurrentHashMap.newKeySet();
      listEncountersInpatient.parallelStream().forEach(enc -> {
        // Check all entries in the location attribute in the Encounter resource for ICU relevance.
        boolean encWithIcu = enc.getLocation().stream().anyMatch(
            loc -> listIcuIds.contains(loc.getLocation().getReference().split("/")[1]));
        if (encWithIcu) {
          listPatientsIcu.add(enc.getPatientId());
        }
      });

      // Get all icu case ids where the patient got at least one positive SARS-CoV-2 lab finding
      Set<String> listPidsIcuPositiveFinding =
          CoronaResultFunctionality.getPidsWithPosCovidLabResult(listUkbObservations)
              .parallelStream().filter(listPatientsIcu::contains)
              .collect(Collectors.toSet());

      logger.debug(
          "Number of patients with ICU and positive SARS-CoV2 PCR test finding: "
              + listPidsIcuPositiveFinding.size());

      // get all cases with at least 1 icu transfer AND an U07.1 or U07.2 diagnosis and add them to the set with the positive lab findings
      listPidsIcuPositiveFinding.addAll(
          CoronaResultFunctionality.getPidsWithCovidDiagnosis(listUkbConditions)
              .parallelStream().filter(listPatientsIcu::contains)
              .collect(Collectors.toSet()));

      logger.debug(
          "Previous list extended by the patients with SARS-CoV2 conditions: "
              + listPidsIcuPositiveFinding.size());

      // now get all the inpatient cases of the patients that fulfilled the criteria
      encounterIdsInput = listEncountersInpatient.parallelStream()
          .filter(x -> listPidsIcuPositiveFinding.contains(x.getPatientId()))
          .map(UkbEncounter::getCaseId).collect(Collectors.toSet());
    } else
    // use total list then
    {
      encounterIdsInput = encounterIds;
    }

    List<List<String>> encounterIdSublists =
        ListHelper.splitList(new ArrayList<>(encounterIdsInput), this.getBatchSize());
    // A query is only useful if at least one encounter id is specified.
    if (encounterIds.size() > 0) {
      encounterIdSublists.parallelStream().forEach(encounterIds -> {
        try {
          List<Bundle.BundleEntryComponent> listTemp = this.getSearchService()
              .getBundleData(new AcuwaveQuerySuffixBuilder().getProcedures(this, encounterIds));
          listTemp.forEach(bundleEntry -> {
            if (bundleEntry.getResource().getResourceType() == ResourceType.Procedure) {
              Procedure procedure = (Procedure) bundleEntry.getResource();
              setProcedures.add(procedure);
            }
          });
        } catch (Exception e) {
          logger.error(
              "Retrieval procedure resources: Unable to build a json module chain: "
                  + e.getMessage());
        }
      });
    } else {
      logger.error(
          "Unable to retrieve procedures resources since no encounter ids could be determined via input filters (Observation + Condition).");
    }

    return new ArrayList<>(setProcedures);
  }

  @Override
  public List<Location> getLocations() {

    // If no case ids could be found, no procedures need to be determined, because the evaluation logic is based on data from the encounter resource.
    if (!locationIdsCouldBeFound(locationIds, ResourceType.Location)) {
      return new ArrayList<>();
    }

    Set<Location> setLocations = ConcurrentHashMap.newKeySet();
    // the location module requires location id as number
    List<Integer> locationIdsNumber =
        locationIds.stream().map(Integer::parseInt).collect(Collectors.toList());
    List<List<Integer>> locationIdSublists =
        ListHelper.splitList(locationIdsNumber, getBatchSize());

    locationIdSublists.parallelStream().forEach(locationIdSublist -> {
      try {
        List<Bundle.BundleEntryComponent> listTemp = this.getSearchService().getBundleData(
            new AcuwaveQuerySuffixBuilder().getLocations(this, locationIdSublist));
        listTemp.forEach(bundleEntry -> {
          if (bundleEntry.getResource().getResourceType() == ResourceType.Location) {
            setLocations.add((Location) bundleEntry.getResource());
          }
        });
      } catch (Exception e) {
        logger.error(
            "Retrieval location resources: Unable to build a json module chain: " + e.getMessage());
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

}
