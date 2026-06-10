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

import static de.ukbonn.mwtek.dashboard.misc.AcribisChecks.calculateValidTimestampsByPid;
import static de.ukbonn.mwtek.dashboard.misc.LoggingHelper.logAbortWorkflowMessage;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.ACRIBIS;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ukbonn.mwtek.dashboard.configuration.CustomGlobalConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.DataItemsConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.ReportsConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.VariantConfiguration;
import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import de.ukbonn.mwtek.dashboard.exceptions.SearchException;
import de.ukbonn.mwtek.dashboard.misc.ProcessTimer;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.AcribisDataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.DataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.PidTimestampCohortMap;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.utilities.fhir.misc.ResourceConverter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiConsent;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiQuestionnaireResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ResourceType;

@Slf4j
public class AcribisDataController {

  public static List<DiseaseDataItem> generateData(
      DataItemContext dataItemContext,
      AbstractDataRetrievalService dataRetrievalService,
      ReportsConfiguration reportConfiguration,
      ProcessTimer processTimer,
      CustomGlobalConfiguration globalConfiguration,
      VariantConfiguration variantConfiguration,
      InputCodeSettings inputCodeSettings,
      QualitativeLabCodesSettings qualitativeLabCodesSettings,
      DataItemsConfiguration dataItemsConfiguration,
      ObjectNode result)
      throws SearchException {

    // Retrieval of the Consent resources
    processTimer.startLoggingTime(ResourceType.Consent);
    // map fhir resources into ukb resources
    // Unusual in the acuwave workflow is
    // that the consent call also gives the valid encounter resources back.
    List<MiiEncounter> miiEncounters = new ArrayList<>();
    // Retrieve consent data and also the valid encounters if acuwave server used
    List<MiiConsent> miiConsents = dataRetrievalService.getConsents(miiEncounters);
    List<MiiCondition> miiConditions = new ArrayList<>();
    List<MiiProcedure> miiProcedures = new ArrayList<>();
    List<MiiPatient> miiPatients = new ArrayList<>();
    List<MiiQuestionnaireResponse> miiQuestionnaireResponses = new ArrayList<>();

    processTimer.stopLoggingTime(miiConsents);

    if (!miiConsents.isEmpty()) {
      // The FHIR search is a combination of encounter id and validity date
      if (globalConfiguration.getServerType() == ServerTypeEnum.FHIR) {
        PidTimestampCohortMap pidTimestampMap = calculateValidTimestampsByPid(miiConsents);
        miiEncounters = dataRetrievalService.getEncounters(pidTimestampMap);
      }
    }

    if (!miiEncounters.isEmpty()) {
      // Retrieval of condition resources
      processTimer.startLoggingTime(ResourceType.Condition);
      miiConditions =
          (List<MiiCondition>)
              ResourceConverter.convert(dataRetrievalService.getConditions(miiEncounters, ACRIBIS));
      processTimer.stopLoggingTime(miiConditions);

      // Retrieval of procedure resources
      miiProcedures = dataRetrievalService.getProcedures(miiEncounters, dataItemContext);
      processTimer.stopLoggingTime(miiProcedures);

      // Retrieval of the Patient resources
      processTimer.startLoggingTime(ResourceType.Patient);
      miiPatients = dataRetrievalService.getPatients(miiProcedures, miiConditions);
      processTimer.stopLoggingTime(miiPatients);

      // Retrieval of the Questionnaire response resources for the follow-up
      processTimer.startLoggingTime(ResourceType.QuestionnaireResponse);
      miiQuestionnaireResponses =
          dataRetrievalService.getQuestionnaireResponses(
              getPatientIdsFromAcribisConsent(miiConsents));
      processTimer.stopLoggingTime(miiQuestionnaireResponses);
    }

    if (miiConsents.isEmpty()) {
      logAbortWorkflowMessage(null, ACRIBIS);
      return new ArrayList<>();
    }
    processTimer.startLoggingTime("Processing logic");

    // Start of the processing logic
    // Formatting of resources in JSON specification
    DataItemGenerator dataItemGenerator =
        new AcribisDataItemGenerator(
            miiConsents,
            miiConditions,
            miiPatients,
            miiEncounters,
            miiProcedures,
            miiQuestionnaireResponses);

    // Creation of the data items of the dataset specification
    List<DiseaseDataItem> dataItems =
        new ArrayList<>(
            dataItemGenerator.getDataItems(
                dataItemsConfiguration.getExcludes(),
                variantConfiguration,
                inputCodeSettings,
                qualitativeLabCodesSettings,
                dataItemContext,
                globalConfiguration));

    // Add resource sizes information to the output if needed
    if (globalConfiguration.getDebug()) {
      result.put("acribisConsentsSize", miiConsents.size());
      result.put("acribisEncountersSize", miiEncounters.size());
      result.put("acribisPatientsSize", miiPatients.size());
      result.put("acribisConditionsSize", miiConditions.size());
      result.put("acribisProceduresSize", miiProcedures.size());
      result.put("acribisQuestionnaireResponsesSize", miiQuestionnaireResponses.size());
    }
    processTimer.stopLoggingTime();

    return dataItems;
  }

  private static List<String> getPatientIdsFromAcribisConsent(List<MiiConsent> miiConsents) {
    return miiConsents.stream()
        .filter(MiiConsent::isAcribisConsentAllowed)
        .map(MiiConsent::getPatientId)
        .toList();
  }
}
