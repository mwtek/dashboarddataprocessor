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

package de.ukbonn.mwtek.dashboard.misc;

import static de.ukbonn.mwtek.utilities.fhir.misc.ResourceConverter.extractReferenceId;

import de.ukbonn.mwtek.dashboard.DashboardApplication;
import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import de.ukbonn.mwtek.dashboard.exceptions.SearchException;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.misc.ResourceConverter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auxiliary class for processing/further processing of FHIR resources.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
public class ResourceHandler {

  static Logger logger = LoggerFactory.getLogger(DashboardApplication.class);

  /**
   * Reads the Fhir bundle, adds all entries of the Observation type to a list and adds the {@link
   * Patient patient} ids and {@link Encounter encounter} ids to a given set.
   *
   * @param bundleResponse A FHIR response bundle that contains {@link Observation} resources
   * @param listObservations List with FHIR-Observations in which the entries from the bundle are to
   *     be stored
   * @param patientIds List of ids of the {@link Patient} resource to be extended by entries from
   *     the Observation resource.
   * @param encounterIds List of ids of the {@link Encounter} resource to be extended by entries
   *     from the Observation resource.
   * @param serverType The connected {@link ServerTypeEnum server type} that delivers the fhir
   *     resources.
   */
  public static void handleObservationEntries(
      Bundle bundleResponse,
      Collection<Observation> listObservations,
      Set<String> patientIds,
      Set<String> encounterIds,
      ServerTypeEnum serverType) {
    bundleResponse
        .getEntry()
        .forEach(
            entry -> {
              if (entry.getResource() instanceof Observation obs) {
                storeObservationPatientKeys(obs, patientIds, encounterIds, serverType);
                listObservations.add(obs);
              }
            });
  }

  /**
   * Class for reading patient and case numbers from {@link
   * de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation} resources and filling global lists
   * with them.
   *
   * @param obs The {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation} resource to be
   *     read.
   * @param outputPatientIds List of ids of the {@link Patient} resource to be extended by entries
   *     from the Observation resource.
   * @param outputEncounterIds List of ids of the {@link Encounter} resource to be extended by
   *     entries from the Observation resource.
   * @param serverType The connected {@link ServerTypeEnum server type} that delivers the fhir
   *     resources.
   */
  public static void storeObservationPatientKeys(
      Observation obs,
      Set<String> outputPatientIds,
      Set<String> outputEncounterIds,
      ServerTypeEnum serverType) {
    try {
      // fhir server usually stores the references in the "resourceType/1234" format during import
      // Until this referencing process (which may never occur), the acuwave server stores its
      // business identifier in the tag 'identifier'
      switch (serverType) {
        case FHIR -> {
          outputPatientIds.add(extractReferenceId(obs.getSubject()));
          outputEncounterIds.add(extractReferenceId(obs.getEncounter()));
        }
        case ACUWAVE -> {
          outputPatientIds.add(obs.getSubject().getIdentifier().getValue());
          outputEncounterIds.add(obs.getEncounter().getIdentifier().getValue());
        }
      }
    } catch (Exception ex) {
      logger.warn(
          "Unable to retrieve the patient or encounter ID for the observation ID: " + obs.getId());
    }
  }

  /**
   * Class for reading patient and case numbers from {@link
   * de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition} resources and filling global lists with
   * them.
   *
   * @param cond The {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition} resource to be
   *     read.
   * @param outputPatientIds List of ids of the {@link Patient} resource to be extended by entries
   *     from the Observation resource.
   * @param outputEncounterIds List of ids of the {@link Encounter} resource to be extended by
   *     entries from the Observation resource.
   * @param serverType The connected {@link ServerTypeEnum server type} that delivers the fhir *
   *     resources.
   */
  public static void storeConditionPatientKeys(
      Condition cond,
      Set<String> outputPatientIds,
      Set<String> outputEncounterIds,
      ServerTypeEnum serverType) {
    try {
      // fhir server usually store the references in the "resourceType/1234" format during import
      // Until this referencing process (which may never occur), the acuwave server stores its
      // business identifier in the tag 'identifier'
      switch (serverType) {
        case FHIR -> {
          outputPatientIds.add(extractReferenceId(cond.getSubject()));
          outputEncounterIds.add(extractReferenceId(cond.getEncounter()));
        }
        case ACUWAVE -> {
          outputPatientIds.add(cond.getSubject().getIdentifier().getValue());
          outputEncounterIds.add(cond.getEncounter().getIdentifier().getValue());
        }
      }
    } catch (Exception ex) {
      logger.warn(
          "Unable to retrieve the patient or encounter ID for the condition ID: " + cond.getId());
    }
  }

  /**
   * Reads the Fhir bundle, adds all entries of the Condition type to a list and adds the {@link
   * Patient patient} ids and {@link Encounter encounter} ids to a given set.
   *
   * @param currentBundle A FHIR response bundle that contains {@link Observation} resources.
   * @param listConditions List with FHIR-{@link Condition conditions} in which the entries from the
   *     bundle are to be stored.
   * @param outputPatientIds List of ids of the {@link Patient} resource to be extended by entries
   *     from the Observation resource.
   * @param outputEncounterIds List of ids of the {@link Encounter} resource to be extended by
   *     entries from the Observation resource.
   * @param serverType The connected {@link ServerTypeEnum server type} that delivers the fhir
   *     resources.
   */
  public static void handleConditionEntries(
      Bundle currentBundle,
      List<Condition> listConditions,
      Set<String> outputPatientIds,
      Set<String> outputEncounterIds,
      ServerTypeEnum serverType) {
    currentBundle
        .getEntry()
        .forEach(
            entry -> {
              if (entry.getResource() instanceof Condition cond) {
                storeConditionPatientKeys(cond, outputPatientIds, outputEncounterIds, serverType);
                listConditions.add(cond);
              }
            });
  }

  /**
   * Storing of the {@link Condition} and {@link Encounter} resources in the submitted lists.
   *
   * @param currentBundle A FHIR response bundle that contains {@link Observation} and {@link
   *     Encounter} resources.
   * @param listConditions List with FHIR-{@link Condition conditions} in which the entries from the
   *     bundle are to be stored.
   * @param listEncounters List with FHIR-{@link Encounter conditions} in which the entries from the
   *     bundle are to be stored and that are referencing on the {@link Condition conditions}.
   * @param serverType The connected {@link ServerTypeEnum server type} that delivers the fhir
   *     resources. At the moment just {@link ServerTypeEnum#FHIR} are supported.
   */
  public static void storeConditionAndEncounterResources(
      Bundle currentBundle,
      List<Condition> listConditions,
      List<Encounter> listEncounters,
      ServerTypeEnum serverType) {
    currentBundle
        .getEntry()
        .forEach(
            entry -> {
              if (entry.getResource() instanceof Condition cond) {
                listConditions.add(cond);
              } else if (entry.getResource() instanceof Encounter enc) {
                listEncounters.add(enc);
              }
            });
  }

  /**
   * Set the Encounter id in the condition resources and add the patient and encounter ids to the
   * passed lists.
   *
   * @param listConditions List with FHIR-{@link Condition conditions} in which the entries from the
   *     bundle are to be stored.
   * @param listEncounters List with FHIR-{@link Encounter conditions} in which the entries from the
   *     bundle are to be stored and that are referencing on the {@link Condition conditions}.
   * @param outputPatientIds List of ids of the {@link Patient} resource to be extended by entries
   *     from the Observation resource.
   * @param outputEncounterIds List of ids of the {@link Encounter} resource to be extended by
   *     entries from the Observation resource.
   * @param serverType The connected {@link ServerTypeEnum server type} that delivers the fhir
   *     resources. At the moment just {@link ServerTypeEnum#FHIR} are supported.
   */
  public static void handleConditionEntriesWithEncounterRefSetting(
      List<Condition> listConditions,
      List<Encounter> listEncounters,
      Set<String> outputPatientIds,
      Set<String> outputEncounterIds,
      ServerTypeEnum serverType) {

    // To simplify the queries and for faster access, we store the primary key of the condition
    // resource in a map and reference the latter.
    Map<String, Condition> conditionMap = new HashMap<>();
    listConditions.forEach(
        cond -> {
          if (cond.getIdElement().hasIdPart()) {
            conditionMap.put(cond.getIdElement().getIdPart(), cond);
          }
        });

    // Iterate over all encounter resources and set the encounter reference in all condition
    // resources that handle disease-positive diagnoses.
    listEncounters.forEach(
        enc -> {
          if (enc.hasDiagnosis()) {
            enc.getDiagnosis()
                .forEach(
                    diagComp -> {
                      String condRefId = extractReferenceId(diagComp.getCondition());
                      // Detection of the disease-positive diagnosis, which are part of the
                      // condition map.
                      if (diagComp.hasCondition()
                          && diagComp.getCondition().hasReference()
                          && conditionMap.containsKey(condRefId)) {
                        Condition cond = conditionMap.get(condRefId);
                        // Setting of the encounter reference in the condition resource.
                        if (enc.getIdElement().hasIdPart()) {
                          Reference encounterReference =
                              new Reference("Encounter/" + enc.getIdElement().getIdPart());
                          cond.setEncounter(encounterReference);
                          storeConditionPatientKeys(
                              cond, outputPatientIds, outputEncounterIds, serverType);
                        }
                      }
                    });
          }
        });
  }

  public static List<CoreBaseDataItem> extractCoreBaseDataOfFacilityEncounters(
      AbstractDataRetrievalService dataRetrievalService) throws SearchException {

    Set<CoreBaseDataItem> coreBaseDataItems = ConcurrentHashMap.newKeySet();
    Set<CoreBaseDataItem> icuEpisodes = ConcurrentHashMap.newKeySet();
    // Just the top level resources are needed
    List<UkbEncounter> facilityContacts =
        ((List<UkbEncounter>) ResourceConverter.convert(dataRetrievalService.getIcuEncounters()))
            .parallelStream().filter(UkbEncounter::isFacilityContact).toList();

    // To detect cases that are currently in ICU, we need to find the active transfers and check
    // for ICU status.
    Set<String> locationIds =
        facilityContacts.stream()
            .flatMap(x -> x.getLocation().stream())
            .collect(Collectors.toSet())
            .stream()
            .map(EncounterLocationComponent::getLocation)
            .filter(Element::hasId)
            .map(Element::getIdBase)
            .collect(Collectors.toSet());

    // Initialize a list with all location ids found
    dataRetrievalService.getLocationIds().addAll(locationIds);

    // Reduce the location ids to the icu wards.
    List<String> icuWardIds =
        ((List<UkbLocation>) ResourceConverter.convert(dataRetrievalService.getLocations()))
            .stream()
                .filter(x -> !x.getType().isEmpty())
                .filter(UkbLocation::isLocationWard)
                .filter(UkbLocation::isLocationIcu)
                .map(Resource::getIdBase)
                .toList();

    facilityContacts.parallelStream()
        .forEach(
            x -> {
              // The main key is the local case id, found in the identifier.
              // Just add an item if it's found.
              if (x.hasIdentifier()) {
                // Set value = 1.0 if the encounter is currently on an icu location
                x.getIdentifier().stream()
                    .filter(y -> y.hasUse() && y.getUse() == IdentifierUse.OFFICIAL)
                    .findFirst()
                    .ifPresent(
                        identifier ->
                            coreBaseDataItems.add(
                                new CoreBaseDataItem(
                                    identifier.getValue(),
                                    null,
                                    x.isCurrentlyOnIcu(icuWardIds) ? 1.0 : 0.0,
                                    x.getPeriod().getStart(),
                                    x.getPeriod().getEnd(),
                                    x.getId())));
              }
            });
    return new ArrayList<>(coreBaseDataItems);
  }
}
