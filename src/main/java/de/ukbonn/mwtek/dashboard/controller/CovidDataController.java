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
package de.ukbonn.mwtek.dashboard.controller;

import static de.ukbonn.mwtek.dashboard.misc.LoggingHelper.addResourceSizesToOutput;
import static de.ukbonn.mwtek.dashboard.misc.LoggingHelper.logAbortWorkflowMessage;
import static de.ukbonn.mwtek.dashboard.misc.ResourceHandler.addDeceasedStatusToEncounters;
import static de.ukbonn.mwtek.dashboard.misc.ResourceHandler.addDummyIcuLocationIfNeeded;
import static de.ukbonn.mwtek.dashboard.misc.ThresholdCheck.filterDataItemsByThreshold;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ukbonn.mwtek.dashboard.configuration.CustomGlobalConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.DataItemsConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.ReportsConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.VariantConfiguration;
import de.ukbonn.mwtek.dashboard.exceptions.SearchException;
import de.ukbonn.mwtek.dashboard.misc.ProcessTimer;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.DataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.utilities.fhir.misc.ResourceConverter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ResourceType;

@Slf4j
public class CovidDataController {

  public static List<DiseaseDataItem> generateData(
      DataItemContext dataItemContext,
      AbstractDataRetrievalService dataRetrievalService,
      ReportsConfiguration reportConfiguration,
      ProcessTimer processTimer,
      CustomGlobalConfiguration customGlobalConfiguration,
      VariantConfiguration variantConfiguration,
      InputCodeSettings inputCodeSettings,
      QualitativeLabCodesSettings qualitativeLabCodesSettings,
      DataItemsConfiguration dataItemsConfiguration,
      ObjectNode result)
      throws SearchException {
    List<DiseaseDataItem> dataItems = new ArrayList<>();

    // Retrieval of the Observation resources
    processTimer.startLoggingTime(ResourceType.Observation);
    List<UkbObservation> ukbObservations =
        (List<UkbObservation>)
            ResourceConverter.convert(dataRetrievalService.getObservations(dataItemContext));
    processTimer.stopLoggingTime(ukbObservations);

    // Retrieval of the Condition resources
    processTimer.startLoggingTime(ResourceType.Condition);
    // map fhir resources into ukb resources
    List<UkbCondition> ukbConditions =
        (List<UkbCondition>)
            ResourceConverter.convert(dataRetrievalService.getConditions(dataItemContext));
    processTimer.stopLoggingTime(ukbConditions);

    // If no conditions or observations were found, the following further data retrievals /
    // calculation steps are irrelevant
    if (!ukbObservations.isEmpty() || !ukbConditions.isEmpty()) {
      // Retrieval of the Patient resources
      processTimer.startLoggingTime(ResourceType.Patient);
      List<UkbPatient> ukbPatients =
          (List<UkbPatient>)
              ResourceConverter.convert(
                  dataRetrievalService.getPatients(
                      ukbObservations, ukbConditions, dataItemContext));
      processTimer.stopLoggingTime(ukbPatients);

      // Retrieval of the Encounter resources
      processTimer.startLoggingTime(ResourceType.Encounter);
      List<UkbEncounter> ukbEncounters =
          (List<UkbEncounter>)
              ResourceConverter.convert(dataRetrievalService.getEncounters(dataItemContext), true);
      processTimer.stopLoggingTime(ukbEncounters);

      // Retrieval of the Location resources
      processTimer.startLoggingTime(ResourceType.Location);
      List<UkbLocation> ukbLocations =
          (List<UkbLocation>) ResourceConverter.convert(dataRetrievalService.getLocations());
      processTimer.stopLoggingTime(ukbLocations);

      // If at least one service provider entry was found or a corresponding contact type
      // => add a dummy icu location
      addDummyIcuLocationIfNeeded(ukbEncounters, ukbLocations);

      // If activated; use Patient.deceasedDateTime for the detection of deceased cases.
      if (customGlobalConfiguration.getUsePatientDeceased())
        addDeceasedStatusToEncounters(ukbPatients, ukbEncounters);

      // Retrieval of the Procedure resources
      List<UkbProcedure> ukbProcedures = new ArrayList<>();
      if (!customGlobalConfiguration.getUseIcuUndifferentiated()) {
        processTimer.startLoggingTime(ResourceType.Procedure);
        ukbProcedures =
            (List<UkbProcedure>)
                ResourceConverter.convert(
                    dataRetrievalService.getProcedures(
                        ukbEncounters,
                        ukbLocations,
                        ukbObservations,
                        ukbConditions,
                        dataItemContext));
        processTimer.stopLoggingTime(ukbProcedures);
      } else
        log.info(
            "Skipping the retrieval of procedure resources, as the generation of"
                + " icu_undifferentiated items is activated.");

      processTimer.startLoggingTime("Processing logic");

      // Start of the processing logic
      // Formatting of resources in json specification
      DataItemGenerator dataItemGenerator =
          new DataItemGenerator(
              ukbConditions,
              ukbObservations,
              ukbPatients,
              ukbEncounters,
              ukbProcedures,
              ukbLocations);

      // Creation of the data items of the dataset specification
      dataItems.addAll(
          dataItemGenerator.getDataItems(
              dataItemsConfiguration.getExcludes(),
              variantConfiguration,
              inputCodeSettings,
              qualitativeLabCodesSettings,
              dataItemContext,
              customGlobalConfiguration));

      // Generate an export with current case/encounter ids by treatment level on demand
      if (reportConfiguration.getCaseIdFileGeneration()) {
        try {
          CoronaResultFunctionality.generateCurrentTreatmentLevelList(
              dataItemGenerator.getMapCurrentTreatmentlevelCasenrs(),
              reportConfiguration.getCaseIdFileDirectory(),
              reportConfiguration.getCaseIdFileBaseName());
        } catch (Exception ex) {
          log.error("Unable to create the crosstab data item. {}", ex.getMessage());
        }
      }

      if (!dataItemsConfiguration.getThresholds().isEmpty()) {
        dataItems = filterDataItemsByThreshold(dataItems, dataItemsConfiguration.getThresholds());
      }

      // Add resource sizes information to the output if needed
      if (customGlobalConfiguration.getDebug()) {
        addResourceSizesToOutput(
            result,
            ukbConditions,
            ukbObservations,
            ukbPatients,
            ukbEncounters,
            ukbLocations,
            ukbProcedures,
            dataItemContext);
      }
      processTimer.stopLoggingTime();
    } else {
      // No conditions or observations found
      logAbortWorkflowMessage(inputCodeSettings, DataItemContext.COVID);
    }
    return dataItems;
  }
}
