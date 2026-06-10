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
import static de.ukbonn.mwtek.dashboard.misc.ResourceHandler.addDummyIcuLocationIfNeeded;
import static de.ukbonn.mwtek.dashboard.misc.ThresholdCheck.filterDataItemsByThreshold;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR_KJP;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR_PED;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ukbonn.mwtek.dashboard.configuration.CustomGlobalConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.DataItemsConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.ReportsConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.VariantConfiguration;
import de.ukbonn.mwtek.dashboard.exceptions.SearchException;
import de.ukbonn.mwtek.dashboard.misc.ProcessTimer;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.DataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.KidsRadarDataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.utilities.fhir.misc.ResourceConverter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ResourceType;

@Slf4j
public class KiRaDataController {

  public static final int MAX_AGE_AT_KJP_MERGE_PROCESS = 20;
  public static final int MAX_AGE_AT_CUT_OFF_DATE = 17;

  public static List<DiseaseDataItem> generateData(
      Set<DataItemContext> dataItemContexts,
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

    List<MiiProcedure> ukbProcedures = new ArrayList<>();
    List<MiiObservation> ukbObservations = new ArrayList<>();

    // Retrieval of the Condition resources
    processTimer.startLoggingTime(ResourceType.Patient);
    // This ped data specific call should include all the patients that we need for kjp as well
    List<MiiPatient> miiPatients =
        dataRetrievalService.getPatients(MAX_AGE_AT_KJP_MERGE_PROCESS, KIDS_RADAR);

    // If no patients were found, the following further data retrievals / calculation steps are
    // irrelevant
    if (!miiPatients.isEmpty()) {

      // Retrieval of the Encounter resources
      processTimer.startLoggingTime(ResourceType.Encounter);
      List<MiiEncounter> miiEncounters =
          (List<MiiEncounter>)
              ResourceConverter.convert(
                  dataRetrievalService.getEncounters(KIDS_RADAR, miiPatients), true);
      processTimer.stopLoggingTime(miiEncounters);

      // Removing patients without inpatient encounters
      Set<String> pidsWithInpatientEncounter =
          miiEncounters.stream().map(MiiEncounter::getPatientId).collect(Collectors.toSet());
      miiPatients =
          miiPatients.stream().filter(x -> pidsWithInpatientEncounter.contains(x.getId())).toList();

      processTimer.startLoggingTime(ResourceType.Condition);
      List<MiiCondition> ukbConditions =
          (List<MiiCondition>)
              ResourceConverter.convert(
                  dataRetrievalService.getConditions(miiEncounters, KIDS_RADAR));
      processTimer.stopLoggingTime(ukbConditions);

      // OBSERVATIONS just needed for ped data; not for kjp
      if (dataItemContexts.contains(KIDS_RADAR_PED) || dataItemContexts.contains(KIDS_RADAR)) {
        processTimer.startLoggingTime(ResourceType.Observation);
        ukbObservations =
            (List<MiiObservation>)
                ResourceConverter.convert(
                    dataRetrievalService.getObservations(
                        miiEncounters,
                        KIDS_RADAR_PED,
                        inputCodeSettings.getKidsRadarPedAllLoincCodes(),
                        qualitativeLabCodesSettings));
        processTimer.stopLoggingTime(ukbObservations);
      } else log.info("Skipping observation retrieval since just kjp data is asked for.");

      // If undifferentiated and ped we don't need to retrieve icu procedure data
      if (dataItemContexts.contains(KIDS_RADAR_KJP)
          || (dataItemContexts.contains(KIDS_RADAR_PED) || dataItemContexts.contains(KIDS_RADAR))
              && !customGlobalConfiguration.getUseIcuUndifferentiated()) {
        processTimer.startLoggingTime(ResourceType.Procedure);
        // Retrieval of kira related procedures
        ukbProcedures = getUkbProcedures(KIDS_RADAR, dataRetrievalService, miiEncounters);
        processTimer.stopLoggingTime(ukbProcedures);
      } else
        log.info(
            "Skipping the retrieval of procedure resources, as the generation of"
                + " icu_undifferentiated items is activated.");

      // Retrieval of the Location resources
      processTimer.startLoggingTime(ResourceType.Location);
      List<MiiLocation> miiLocations =
          (List<MiiLocation>) ResourceConverter.convert(dataRetrievalService.getLocations());
      processTimer.stopLoggingTime(miiLocations);

      // If at least one service provider entry was found or a corresponding contact type
      // => add a dummy icu location
      addDummyIcuLocationIfNeeded(miiEncounters, miiLocations);

      processTimer.startLoggingTime("Processing logic");

      // Start of the processing logic
      // Formatting of resources in json specification
      DataItemGenerator dataItemGenerator =
          new KidsRadarDataItemGenerator(
              ukbConditions,
              ukbObservations,
              miiPatients,
              miiEncounters,
              ukbProcedures,
              miiLocations);

      // Creation of the data items of the dataset specification
      List<DataItemContext> contextsToProcess = new ArrayList<>();

      if (dataItemContexts.contains(KIDS_RADAR)) {
        // KIDS_RADAR implies both PED and KJP
        contextsToProcess.add(KIDS_RADAR_PED);
        contextsToProcess.add(KIDS_RADAR_KJP);
      } else {
        if (dataItemContexts.contains(KIDS_RADAR_PED)) {
          contextsToProcess.add(KIDS_RADAR_PED);
        }
        if (dataItemContexts.contains(KIDS_RADAR_KJP)) {
          contextsToProcess.add(KIDS_RADAR_KJP);
        }
      }

      // Add data items for all relevant contexts
      for (DataItemContext context : contextsToProcess) {
        dataItems.addAll(
            dataItemGenerator.getDataItems(
                dataItemsConfiguration.getExcludes(),
                variantConfiguration,
                inputCodeSettings,
                qualitativeLabCodesSettings,
                context,
                customGlobalConfiguration));
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
            miiPatients,
            miiEncounters,
            miiLocations,
            ukbProcedures,
            KIDS_RADAR);
      }
      processTimer.stopLoggingTime();
    } else {
      // No conditions or observations found
      logAbortWorkflowMessage(inputCodeSettings, KIDS_RADAR);
    }
    return dataItems;
  }

  /**
   * Retrieves ICU-related procedures for the provided encounters based on the underlying server
   * type of the {@link AbstractDataRetrievalService}.
   *
   * @param dataItemContext context influencing the retrieval (e.g., filters/profiles)
   * @param dataRetrievalService data source abstraction (FHIR or ACUWAVE)
   * @param miiEncounters encounters to derive case/facility-contact IDs from
   * @return a list of {@link MiiProcedure} objects
   * @throws ClassCastException if the converted payload is not a {@code List<UkbProcedure>}
   */
  @SuppressWarnings("unchecked")
  private static List<MiiProcedure> getUkbProcedures(
      DataItemContext dataItemContext,
      AbstractDataRetrievalService dataRetrievalService,
      List<MiiEncounter> miiEncounters) {
    List<MiiProcedure> ukbProcedures = new ArrayList<>();
    switch (dataRetrievalService.getServerType()) {
      case FHIR -> {
        ukbProcedures =
            (List<MiiProcedure>)
                ResourceConverter.convert(
                    dataRetrievalService.getProcedures(
                        miiEncounters.stream().filter(MiiEncounter::isFacilityContact).toList(),
                        dataItemContext,
                        null));
      }
      // Acuwave asks two different servers relaying whether archive data or live data is needed.
      case ACUWAVE -> {
        ukbProcedures =
            (List<MiiProcedure>)
                ResourceConverter.convert(
                    dataRetrievalService.getProcedures(
                        miiEncounters.stream().filter(x -> !x.isActive()).toList(),
                        dataItemContext,
                        false));
        // CLAPP calls to get live data
        ukbProcedures.addAll(
            (List<MiiProcedure>)
                ResourceConverter.convert(
                    dataRetrievalService.getProcedures(
                        miiEncounters.stream().filter(MiiEncounter::isActive).toList(),
                        dataItemContext,
                        true)));
      }
    }
    return ukbProcedures;
  }
}
