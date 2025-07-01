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

import static de.ukbonn.mwtek.utilities.fhir.mapping.kdscase.valuesets.KdsEncounterFixedValues.DEATH_CODE;
import static de.ukbonn.mwtek.utilities.fhir.mapping.kdscase.valuesets.KdsEncounterFixedValues.DEATH_CODE_DISPLAY;
import static de.ukbonn.mwtek.utilities.fhir.mapping.kdscase.valuesets.KdsEncounterFixedValues.DISCHARGE_DISPOSITION_EXT_URL;
import static de.ukbonn.mwtek.utilities.fhir.mapping.kdscase.valuesets.KdsEncounterFixedValues.DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_EXT_URL;
import static de.ukbonn.mwtek.utilities.fhir.mapping.kdscase.valuesets.KdsEncounterFixedValues.DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_SYSTEM;
import static de.ukbonn.mwtek.utilities.fhir.misc.LocationTools.isDummyIcuLocation;
import static de.ukbonn.mwtek.utilities.fhir.misc.ResourceConverter.extractReferenceId;

import de.ukbonn.mwtek.dashboard.DashboardApplication;
import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import de.ukbonn.mwtek.dashboard.exceptions.SearchException;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.misc.LocationTools;
import de.ukbonn.mwtek.utilities.fhir.misc.ResourceConverter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbConsent;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterHospitalizationComponent;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Procedure.ProcedureStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auxiliary class for processing/further processing of FHIR resources.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public class ResourceHandler {

  static Logger logger = LoggerFactory.getLogger(DashboardApplication.class);

  /**
   * Reads the Fhir bundle, adds all entries of the Observation type to a list and adds the {@link
   * Patient patient} ids and {@link Encounter encounter} ids to a given set.
   *
   * @param bundleResponse A FHIR response bundle that contains {@link Observation} resources
   * @param observations List with FHIR-Observations in which the entries from the bundle are to be
   *     stored
   * @param patientIds List of ids of the {@link Patient} resource to be extended by entries from
   *     the Observation resource.
   * @param encounterIds List of ids of the {@link Encounter} resource to be extended by entries
   *     from the Observation resource.
   * @param serverType The connected {@link ServerTypeEnum server type} that delivers the fhir
   *     resources.
   */
  public static void handleObservationEntries(
      Bundle bundleResponse,
      Collection<Observation> observations,
      Set<String> patientIds,
      Set<String> encounterIds,
      ServerTypeEnum serverType) {
    bundleResponse
        .getEntry()
        .forEach(
            entry -> {
              if (entry.getResource() instanceof Observation obs) {
                // Filtering of canceled / entered-in-error resources
                if (isObservationStatusValid(obs)) {
                  storeObservationPatientKeys(
                      removeNotNeededAttributes(obs), patientIds, encounterIds, serverType);
                  observations.add(obs);
                }
              }
            });
  }

  /** Filtering of canceled + entered-in-error observations. */
  private static boolean isObservationStatusValid(Observation observation) {
    if (!observation.hasStatus()) return false;
    else {
      var observationStatus = observation.getStatus();
      return observationStatus != ObservationStatus.CANCELLED
          && observationStatus != ObservationStatus.ENTEREDINERROR;
    }
  }

  /** Removing non-needed attributes to optimize the heap space usage. */
  public static Observation removeNotNeededAttributes(Observation obs) {
    obs.setMeta(null);
    obs.setCategory(null);
    obs.setIdentifier(null);
    obs.setInterpretation(null);
    return obs;
  }

  /** Removing non-needed attributes to optimize the heap space usage. */
  public static Encounter removeNotNeededAttributes(Encounter enc) {
    enc.setMeta(null);
    enc.setExtension(null);
    return enc;
  }

  /** Removing non-needed attributes to optimize the heap space usage. */
  public static Condition removeNotNeededAttributes(Condition cond) {
    cond.setMeta(null);
    cond.setClinicalStatus(null);
    cond.setExtension(null);
    return cond;
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
          "Unable to retrieve the patient or encounter ID for the observation ID: {}", obs.getId());
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
          "Unable to retrieve the patient or encounter ID for the condition ID: {}", cond.getId());
    }
  }

  /**
   * Reads the Fhir bundle, adds all entries of the Condition type to a list and adds the {@link
   * Patient patient} ids and {@link Encounter encounter} ids to a given set.
   *
   * @param currentBundle A FHIR response bundle that contains {@link Observation} resources.
   * @param conditions List with FHIR-{@link Condition conditions} in which the entries from the
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
      List<Condition> conditions,
      Set<String> outputPatientIds,
      Set<String> outputEncounterIds,
      ServerTypeEnum serverType) {
    currentBundle
        .getEntry()
        .forEach(
            entry -> {
              if (entry.getResource() instanceof Condition cond) {
                storeConditionPatientKeys(cond, outputPatientIds, outputEncounterIds, serverType);
                conditions.add(removeNotNeededAttributes(cond));
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

  public static void handleConsentEntries(
      Bundle currentBundle,
      List<UkbConsent> consents,
      Collection<UkbEncounter> encountersOutput,
      Set<String> outputPatientIds,
      ServerTypeEnum serverType) {
    currentBundle
        .getEntry()
        .forEach(
            entry -> {
              addValidConsentEntries(
                  consents, encountersOutput, outputPatientIds, serverType, entry);
            });
  }

  public static void addValidConsentEntries(
      Collection<UkbConsent> consents,
      Collection<UkbEncounter> encountersOutput,
      Set<String> outputPatientIds,
      ServerTypeEnum serverType,
      BundleEntryComponent entry) {
    if (entry.getResource() instanceof Consent consent) {
      try {
        UkbConsent ukbConsent = (UkbConsent) ResourceConverter.convert(consent, true);
        if (isConsentValid(ukbConsent)) {
          storeConsentPatientKeys(ukbConsent, outputPatientIds, serverType);
          consents.add(ukbConsent);
        } else {
          log.debug("Consent usage not allowed for consent id: {}", ukbConsent.getId());
        }
      } catch (Exception ex) {
        logMissingCoreAttribute(consent, ex);
      }
    } else if (entry.getResource() instanceof Encounter encounter) {
      // We just need the facility contacts
      try {
        UkbEncounter ukbEncounter = (UkbEncounter) ResourceConverter.convert(encounter, true);
        if (ukbEncounter.isFacilityContact()) {
          encountersOutput.add(ukbEncounter);
        }
      } catch (Exception ex) {
        log.warn(
            "Error while converting encounter {} because: {}", encounter.getId(), ex.getMessage());
      }
    }
  }

  private static void logMissingCoreAttribute(Consent consent, Exception ex) {
    log.warn(
        "Resource {} not usable since {} attribute is missing.", consent.getId(), ex.getMessage());
  }

  private static boolean isConsentValid(UkbConsent ukbConsent) {
    // Prefiltering to consents that gave permission to use data
    return ukbConsent.isPatDataUsageAllowed() || ukbConsent.isAcribisConsentAllowed();
  }

  public static boolean isEncounterInpatientFacilityContact(UkbEncounter ukbEncounter) {
    return (ukbEncounter.isFacilityContact()
        && (ukbEncounter.isCaseClassInpatientOrShortStay()
            || ukbEncounter.isCaseTypePostStationary()));
  }

  public static void storeConsentPatientKeys(
      Consent consent, Set<String> outputPatientIds, ServerTypeEnum serverType) {
    try {
      // fhir server usually store the references in the "resourceType/1234" format during import
      // Until this referencing process (which may never occur), the acuwave server stores its
      // business identifier in the tag 'identifier'
      switch (serverType) {
        case FHIR -> {
          outputPatientIds.add(extractReferenceId(consent.getPatient()));
        }
        case ACUWAVE -> {
          outputPatientIds.add(consent.getPatient().getIdentifier().getValue());
        }
      }
    } catch (Exception ex) {
      logger.warn("Unable to retrieve the patient ID for the consent ID: {}", consent.getId());
    }
  }

  /**
   * If at least one service provider entry was found in the encounter list the location list its
   * possible that the icu transfer information is getting managed via this attribute. If so, we
   * create references to a dummy icu location.
   */
  public static void addDummyIcuLocationIfNeeded(
      List<UkbEncounter> ukbEncounters, List<UkbLocation> ukbLocations) {
    boolean isServiceProviderUsed = ukbEncounters.stream().anyMatch(Encounter::hasServiceProvider);

    boolean isDummyIcuLocationReferencePresent =
        ukbEncounters.parallelStream()
            .filter(Encounter::hasLocation)
            .flatMap(encounter -> encounter.getLocation().stream())
            .anyMatch(location -> isDummyIcuLocation(location.getLocation()));

    if (isServiceProviderUsed || isDummyIcuLocationReferencePresent) {
      ukbLocations.add(LocationTools.createDummyIcuWardLocation());
      log.info(
          "Dummy ICU location resource was added because service provider entries were found or a "
              + "resource with Encounter.type.kontaktart = 'intensivstationaer' "
              + "but without locations was found.");
    }
  }

  /**
   * Adds a discharge disposition extension indicating "deceased" to encounters where the patient
   * has died.
   *
   * <p>This method first creates a map of deceased patients based on their ID and date of death.
   * Then, it filters encounters that belong to these patients and checks whether the encounter
   * period includes the deceased date. If the conditions match, a discharge disposition extension
   * is added to the encounter.
   *
   * @param ukbPatients The list of {@link UkbPatient} containing patient records.
   * @param ukbEncounters The list of {@link UkbEncounter} representing encounters to be processed.
   */
  public static void addDeceasedStatusToEncounters(
      List<UkbPatient> ukbPatients, List<UkbEncounter> ukbEncounters) {

    // Create a map with Patient-ID as Key and DeceasedDateTimeType as Value
    Map<String, DateTimeType> deceasedPatientsMap =
        ukbPatients.parallelStream()
            .filter(Patient::hasDeceasedDateTimeType)
            .collect(Collectors.toMap(UkbPatient::getId, UkbPatient::getDeceasedDateTimeType));

    // Filter relevant UkbEncounters
    List<UkbEncounter> encountersDeceased =
        ukbEncounters.stream()
            .filter(
                encounter -> {
                  DateTimeType deceasedDate = deceasedPatientsMap.get(encounter.getPatientId());
                  return deceasedDate != null
                      && encounter.isFacilityContact()
                      && encounter.hasPeriod()
                      && DateTools.isWithinPeriod(deceasedDate.getValue(), encounter.getPeriod());
                })
            .toList();

    encountersDeceased.forEach(ResourceHandler::addDischargeDispositionDeadToEncounter);
    log.info(
        "Added 'dischargeDispositions = dead' extensions to {} 'einrichtungskontakt' encounters.",
        encountersDeceased.size());
  }

  // Static extension to avoid redundant object creation
  private static final Extension DISCHARGE_REASON_EXTENSION;

  static {
    // Initialize the static discharge reason extension
    DISCHARGE_REASON_EXTENSION = new Extension(DISCHARGE_DISPOSITION_EXT_URL);
    Extension subExtFirstAndSecPos =
        new Extension(DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_EXT_URL);
    subExtFirstAndSecPos.setValue(
        new Coding(
            DISCHARGE_DISPOSITION_FIRST_AND_SECOND_POS_SYSTEM, DEATH_CODE, DEATH_CODE_DISPLAY));
    DISCHARGE_REASON_EXTENSION.addExtension(subExtFirstAndSecPos);
  }

  /**
   * Sets the discharge disposition of the given encounter to "deceased".
   *
   * <p>This method always overwrites any existing hospitalization component with a new one
   * containing the discharge disposition extension indicating death.
   *
   * @param encounter The {@link UkbEncounter} to update with a "deceased" discharge disposition.
   */
  public static void addDischargeDispositionDeadToEncounter(UkbEncounter encounter) {
    // Create a new hospitalization component
    EncounterHospitalizationComponent hospitalizationComponent =
        new EncounterHospitalizationComponent();

    // Create a new discharge disposition and reuse the pre-built extension
    CodeableConcept dischargeDisposition = new CodeableConcept();
    dischargeDisposition.addExtension(DISCHARGE_REASON_EXTENSION);

    // Assign the discharge disposition to the hospitalization component
    hospitalizationComponent.setDischargeDisposition(dischargeDisposition);

    // Overwrite the existing hospitalization component of the encounter
    encounter.setHospitalization(hospitalizationComponent);
  }

  /** Filtering of not-done + entered-in-error observations. */
  public static boolean isProcedureStatusValid(Procedure procedure) {
    // If status is empty it is a non-valid resource
    if (!procedure.hasStatus()) return false;
    else {
      var procedureStatus = procedure.getStatus();
      return procedureStatus != ProcedureStatus.ENTEREDINERROR
          && procedureStatus != ProcedureStatus.NOTDONE;
    }
  }

  public static List<String> getPidsByProceduresAndConditions(
      List<UkbProcedure> ukbProcedures, List<UkbCondition> ukbConditions) {
    return Stream.concat(
            ukbProcedures.stream().map(UkbProcedure::getPatientId),
            ukbConditions.stream().map(UkbCondition::getPatientId))
        .distinct()
        .toList();
  }
}
