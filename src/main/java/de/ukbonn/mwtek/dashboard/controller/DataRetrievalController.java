/*
 *  Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 *  modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 *  PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 *  OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 *  IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 *  YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 *  OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 *  COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 *  BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 *  OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 *  PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */
package de.ukbonn.mwtek.dashboard.controller;

import static de.ukbonn.mwtek.dashboard.controller.UkbModelController.generateUkbModelData;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ukbonn.mwtek.dashboard.CoronaDashboardApplication;
import de.ukbonn.mwtek.dashboard.configuration.AcuwaveSearchConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.AcuwaveServerRestConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.ExcludeDataItemsConfigurations;
import de.ukbonn.mwtek.dashboard.configuration.FhirSearchConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.GlobalConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.PredictionModelsConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.ReportsConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.VariantConfiguration;
import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import de.ukbonn.mwtek.dashboard.exceptions.SearchException;
import de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer;
import de.ukbonn.mwtek.dashboard.misc.ProcessTimer;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.AcuwaveDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.AcuwaveSearchService;
import de.ukbonn.mwtek.dashboard.services.FhirDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.FhirSearchService;
import de.ukbonn.mwtek.dashboard.services.ProviderService;
import de.ukbonn.mwtek.dashboardlogic.CoronaDataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.RenalReplacementRiskDataGenerator;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.utilities.fhir.misc.Converter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Controller class for the "/createJson" REST endpoint with the main method to create the Json
 * specification for the Corona Dashboard.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
@Controller
@RequestMapping("/createJson")
public class DataRetrievalController {

  /**
   * Number of days since discharge for which the data should be loaded from reporting instead of
   * from Clapp
   */
  public static final int DAYS_DIFFERENCE = 3;
  public static final String WORKFLOW_ABORTED = "Workflow aborted: ";
  Logger logger = LoggerFactory.getLogger(CoronaDashboardApplication.class);

  private final AcuwaveSearchService acuwaveSearchService;
  private final FhirSearchService fhirSearchService;

  private final ProviderService providerService;
  @Autowired
  private GlobalConfiguration globalConfiguration;
  @Autowired
  private ExcludeDataItemsConfigurations exclDataItems;
  @Autowired
  private FhirSearchConfiguration fhirSearchConfiguration;
  @Autowired
  private ReportsConfiguration reportConfiguration;
  @Autowired
  private AcuwaveServerRestConfiguration acuwaveServerConfiguration;
  @Autowired
  private AcuwaveSearchConfiguration acuwaveSearchConfiguration;
  @Autowired
  private VariantConfiguration variantConfiguration;
  @Autowired
  private PredictionModelsConfiguration predictionModelsConfiguration;

  @Autowired
  public DataRetrievalController(AcuwaveSearchService acuwaveSearchService,
      FhirSearchService fhirSearchService, ProviderService providerService) {
    this.acuwaveSearchService = acuwaveSearchService;
    this.fhirSearchService = fhirSearchService;
    this.providerService = providerService;
  }

  /**
   * The result size of the REST response
   */
  protected long resultSize = 0;
  protected InputStream resultStream = null;

  /**
   * REST endpoint (/createJson) that handles FHIR server data retrieval as well as processing in
   * Ukb resources, calling dashboard logic and providing Json output.
   *
   * @return String with the json corona dashboard specification or alternatively an error message
   */
  @SuppressWarnings("unchecked")
  @GetMapping
  public ResponseEntity<String> createJson() {

    // initialize new request
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode result = new ObjectMapper().createObjectNode();

    // Result
    List<CoronaDataItem> dataItems = new ArrayList<>();

    // timer for benchmarking purposes
    ProcessTimer processTimer = new ProcessTimer();

    boolean benchMarkRun = false;

    resultStream = null;

    // Determine the data retrieval service for the server type used
    AbstractDataRetrievalService dataRetrievalService;
    if (globalConfiguration.getServerType() == ServerTypeEnum.ACUWAVE) {
      dataRetrievalService = new AcuwaveDataRetrievalService(acuwaveSearchService,
          this.acuwaveSearchConfiguration, this.globalConfiguration);
    } else {
      dataRetrievalService =
          new FhirDataRetrievalService(fhirSearchService, this.fhirSearchConfiguration,
              this.globalConfiguration);
    }

    boolean skipBaseDataGeneration = dataRetrievalService.getGlobalConfiguration()
        .getSkipBaseDataGeneration();
    //
    //    // debug mode (extended output)
    boolean debugMode = this.globalConfiguration.getDebug();
    //
    //    // If custom codes are set in the yaml file -> update the default values.
    InputCodeSettings inputCodeSettings = ConfigurationTransformer.extractInputCodeSettings(
        dataRetrievalService);

    try {
      // Starting with the calculation of the prediction models to release the memory right after.
      if (predictionModelsConfiguration.getUkbRenalReplacementProcedures()) {
        Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>> renalReplacementModelParameterSetMap = generateUkbModelData(
            benchMarkRun, dataRetrievalService, processTimer, globalConfiguration.getServerType());

        // Postprocessing
        dataItems.addAll(RenalReplacementRiskDataGenerator.generateDataItems(
            renalReplacementModelParameterSetMap));
      }

      if (!skipBaseDataGeneration) {
        // Retrieval of the Observation resources
        processTimer.startLoggingTime(ResourceType.Observation);
        List<UkbObservation> listUkbObservations =
            (List<UkbObservation>) Converter.convert(dataRetrievalService.getObservations());
        processTimer.stopLoggingTime(listUkbObservations);

        // Retrieval of the Condition resources
        processTimer.startLoggingTime(ResourceType.Condition);
        // map fhir resources into ukb resources
        List<UkbCondition> listUkbConditions =
            (List<UkbCondition>) Converter.convert(dataRetrievalService.getConditions());
        processTimer.stopLoggingTime(listUkbConditions);

        // If no conditions or observations were found, the following further data retrievals / calculation steps are irrelevant
        if (listUkbObservations.size() > 0 && listUkbConditions.size() > 0) {

          // Retrieval of the Patient resources
          processTimer.startLoggingTime(ResourceType.Patient);
          List<UkbPatient> listUkbPatients = (List<UkbPatient>) Converter.convert(
              dataRetrievalService.getPatients(listUkbObservations, listUkbConditions));
          processTimer.stopLoggingTime(listUkbPatients);

          // Retrieval of the Encounter resources
          processTimer.startLoggingTime(ResourceType.Encounter);
          List<UkbEncounter> listUkbEncounters =
              (List<UkbEncounter>) Converter.convert(dataRetrievalService.getEncounters(), true);
          processTimer.stopLoggingTime(listUkbEncounters);

          // Retrieval of the Location resources
          processTimer.startLoggingTime(ResourceType.Location);
          List<UkbLocation> listUkbLocations =
              (List<UkbLocation>) Converter.convert(dataRetrievalService.getLocations());
          processTimer.stopLoggingTime(listUkbLocations);

          // Retrieval of the Procedure resources
          processTimer.startLoggingTime(ResourceType.Procedure);
          List<UkbProcedure> listUkbProcedures = (List<UkbProcedure>) Converter.convert(
              dataRetrievalService.getProcedures(listUkbEncounters, listUkbLocations,
                  listUkbObservations, listUkbConditions));
          processTimer.stopLoggingTime(listUkbProcedures);

          processTimer.startLoggingTime("Processing logic");

          // Start of the processing logic
          // Formatting of resources in json specification
          CoronaDataItemGenerator coronaDataItemGenerator =
              new CoronaDataItemGenerator(listUkbConditions, listUkbObservations, listUkbPatients,
                  listUkbEncounters, listUkbProcedures, listUkbLocations);

          // Creation of the data items of the dataset specification
          dataItems.addAll(
              coronaDataItemGenerator.getDataItems(exclDataItems.getExcludes(), debugMode,
                  variantConfiguration, inputCodeSettings));

          // Generate an export with current case/encounter ids by treatment level on demand
          if (reportConfiguration.getCaseIdFileGeneration()) {
            CoronaResultFunctionality.generateCurrentTreatmentLevelList(
                coronaDataItemGenerator.getMapCurrentTreatmentlevelCasenrs(),
                reportConfiguration.getCaseIdFileDirectory(),
                reportConfiguration.getCaseIdFileBaseName());
          }

          processTimer.stopLoggingTime();

          if (debugMode) {
            // put the sizes of each fhir resource list in the output
            result.put("conditionSize", listUkbConditions.size());
            result.put("observationSize", listUkbObservations.size());
            result.put("patientSize", listUkbPatients.size());
            result.put("encounterSize", listUkbEncounters.size());
            result.put("locationSize", listUkbLocations.size());
            result.put("procedureSize", listUkbProcedures.size());
          }
        } else {
          // No conditions or observations found
          String abortMessage = "Unable to find any covid related observations (loinc codes:"
              + inputCodeSettings.getObservationPcrLoincCodes() + ") or conditions (icd codes: "
              + inputCodeSettings.getConditionIcdCodes() + ").";
          logger.error(WORKFLOW_ABORTED + abortMessage);
          return new ResponseEntity<>(abortMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
      }

      ArrayNode dataItemsArrayNode = mapper.valueToTree(dataItems);
      result.putArray("dataitems").addAll(dataItemsArrayNode);
      result.put("provider", this.providerService.provConf.getName());
      result.put("corona_dashboard_dataset_version", "0.5.0");
      result.put("author", this.providerService.provConf.getAuthor());
      result.put("exporttimestamp", DateTools.getCurrentUnixTime());

      byte[] resultBuffer = result.toString().getBytes(StandardCharsets.UTF_8);
      this.resultSize = resultBuffer.length;
      this.resultStream = new ByteArrayInputStream(resultBuffer);

      return new ResponseEntity<>(result.toString(), HttpStatus.OK);
    } catch (SearchException ex) {
      // Log the error message with more information
      logger.error(WORKFLOW_ABORTED + ex.getMessage());
      return new ResponseEntity<>("Error occurred while requesting data:\n\n " + ex.getMessage(),
          HttpStatus.INTERNAL_SERVER_ERROR);
    } catch
    (HttpClientErrorException ex) {
      // Wrong credentials
      logger.error(WORKFLOW_ABORTED + ex.getMessage());
      return new ResponseEntity<>("Connection to the FHIR server failed:\n\n " + ex.getMessage(),
          ex.getStatusCode());
    } catch (HttpServerErrorException ex) {
      // Server unavailable
      logger.error(WORKFLOW_ABORTED + ex.getMessage());
      return new ResponseEntity<>(
          "Error in the data retrieval from the FHIR server:\n\n " + ex.getMessage(),
          HttpStatus.valueOf(ex.getRawStatusCode()));
    } catch (ResourceAccessException ex) {
      logger.error(WORKFLOW_ABORTED + ex.getMessage());
      return new ResponseEntity<>(
          "Connection to the FHIR/Acuwave server failed:\n\n " + ex.getMessage(),
          HttpStatus.SERVICE_UNAVAILABLE);
    } catch (Exception ex) {
      logger.error(WORKFLOW_ABORTED + ex.getMessage());
      return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}
