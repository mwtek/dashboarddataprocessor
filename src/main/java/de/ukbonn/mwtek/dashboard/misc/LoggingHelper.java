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

import static de.ukbonn.mwtek.dashboard.controller.DataRetrievalController.WORKFLOW_ABORTED;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;

@Slf4j
public class LoggingHelper {

  public static void logParsingResourceException(Exception ex, String context) {
    log.warn(
        "Parsing a bundle failed with reason: {}. Probably the results are empty for query: {}",
        ex.getMessage(),
        context);
  }

  public static void logWarningForUnexpectedResource(
      Class<? extends DomainResource> expectedResource, Bundle.BundleEntryComponent bundleEntry) {
    log.warn(
        "Expected an {} resource in the response of the body weight query but found: {} with id {}",
        expectedResource.getName(),
        bundleEntry.getResource().fhirType(),
        bundleEntry.getResource().getId());
  }

  static Boolean covidWorkflowAborted = false;
  static Boolean influenzaWorkflowAborted = false;
  static Boolean kidsRadarWorkflowAborted = false;
  @Getter static String abortMessage;

  public static void logAbortWorkflowMessage(
      InputCodeSettings inputCodeSettings, DataItemContext dataItemContext) {
    List<String> pcrCodes = new ArrayList<>();
    List<String> icdCodes = new ArrayList<>();
    switch (dataItemContext) {
      case COVID -> {
        pcrCodes = inputCodeSettings.getCovidObservationPcrLoincCodes();
        icdCodes = inputCodeSettings.getCovidConditionIcdCodes();
        covidWorkflowAborted = true;
      }
      case INFLUENZA -> {
        pcrCodes = inputCodeSettings.getInfluenzaObservationPcrLoincCodes();
        icdCodes = inputCodeSettings.getInfluenzaConditionIcdCodes();
        influenzaWorkflowAborted = true;
      }
      case KIDS_RADAR -> {
        // No observation codes here; just icd codes
        icdCodes =
            inputCodeSettings.getKidsRadarConditionKjpIcdCodes().values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        icdCodes.addAll(
            inputCodeSettings.getKidsRadarConditionRsvIcdCodes().values().stream()
                .flatMap(List::stream)
                .toList());
        kidsRadarWorkflowAborted = true;
      }
    }
    abortMessage =
        "Unable to find any "
            + dataItemContext
            + " related observations (loinc codes:"
            + pcrCodes
            + ") or conditions (icd codes: "
            + icdCodes
            + ").";
    log.error(WORKFLOW_ABORTED + "{}", abortMessage);
  }

  /** Was the workflow of a context interrupted for any reason? */
  public static boolean gotWorkflowAborted(DataItemContext dataItemContext) {
    switch (dataItemContext) {
      case COVID -> {
        return covidWorkflowAborted;
      }
      case INFLUENZA -> {
        return influenzaWorkflowAborted;
      }
    }
    return false;
  }

  public static void addResourceSizesToOutput(
      ObjectNode result,
      List<UkbCondition> ukbConditions,
      List<UkbObservation> ukbObservations,
      List<UkbPatient> ukbPatients,
      List<UkbEncounter> ukbEncounters,
      List<UkbLocation> ukbLocations,
      List<UkbProcedure> ukbProcedures,
      DataItemContext dataItemContext) {
    String contextPrefix = "";
    switch (dataItemContext) {
      case COVID -> contextPrefix = "covid";
      case INFLUENZA -> contextPrefix = "influenza";
      case KIDS_RADAR -> contextPrefix = "kira";
    }
    result.put(contextPrefix + "ConditionSize", ukbConditions.size());
    if (ukbObservations != null) {
      result.put(contextPrefix + "ObservationSize", ukbObservations.size());
    }
    result.put(contextPrefix + "PatientSize", ukbPatients.size());
    result.put(contextPrefix + "EncounterSize", ukbEncounters.size());
    if (ukbLocations != null) {
      result.put(contextPrefix + "LocationSize", ukbLocations.size());
    }
    if (ukbProcedures != null) {
      result.put(contextPrefix + "ProcedureSize", ukbProcedures.size());
    }
  }
}
