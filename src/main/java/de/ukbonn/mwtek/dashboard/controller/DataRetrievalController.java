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

import static de.ukbonn.mwtek.dashboard.controller.UkbModelController.generateUkbModelData;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.COVID;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.INFLUENZA;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ukbonn.mwtek.dashboard.DashboardApplication;
import de.ukbonn.mwtek.dashboard.configuration.AcuwaveSearchConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.AcuwaveServerRestConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.ExcludeDataItemsConfigurations;
import de.ukbonn.mwtek.dashboard.configuration.FhirSearchConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.GlobalConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.ReportsConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.VariantConfiguration;
import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import de.ukbonn.mwtek.dashboard.exceptions.SearchException;
import de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer;
import de.ukbonn.mwtek.dashboard.misc.LoggingHelper;
import de.ukbonn.mwtek.dashboard.misc.ProcessTimer;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.AcuwaveDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.AcuwaveSearchService;
import de.ukbonn.mwtek.dashboard.services.FhirDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.FhirSearchService;
import de.ukbonn.mwtek.dashboard.services.ProviderService;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.RenalReplacementRiskDataGenerator;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.enums.RenalReplacementRiskParameters;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
  public static final String CURRENT_DATASET_VERSION = "0.5.4";
  public static final String FILE_GENERATOR = "ddp";
  Logger logger = LoggerFactory.getLogger(DashboardApplication.class);

  private final AcuwaveSearchService acuwaveSearchService;
  private final FhirSearchService fhirSearchService;

  private final ProviderService providerService;
  @Autowired private GlobalConfiguration globalConfiguration;
  @Autowired private ExcludeDataItemsConfigurations exclDataItems;
  @Autowired private FhirSearchConfiguration fhirSearchConfiguration;
  @Autowired private ReportsConfiguration reportConfiguration;
  @Autowired private AcuwaveServerRestConfiguration acuwaveServerConfiguration;
  @Autowired private AcuwaveSearchConfiguration acuwaveSearchConfiguration;
  @Autowired private VariantConfiguration variantConfiguration;

  @Autowired
  public DataRetrievalController(
      AcuwaveSearchService acuwaveSearchService,
      FhirSearchService fhirSearchService,
      ProviderService providerService) {
    this.acuwaveSearchService = acuwaveSearchService;
    this.fhirSearchService = fhirSearchService;
    this.providerService = providerService;
  }

  /** The result size of the REST response */
  protected long resultSize = 0;

  protected InputStream resultStream = null;

  /**
   * REST endpoint (/createJson) that handles FHIR server data retrieval as well as processing in
   * Ukb resources, calling dashboard logic and providing Json output.
   *
   * @return String with the json corona dashboard specification or alternatively an error message
   */
  @GetMapping
  public ResponseEntity<String> createJson(@RequestParam(required = false) List<String> scopes) {

    // initialize new request
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode result = new ObjectMapper().createObjectNode();

    // Result
    List<DiseaseDataItem> dataItems = new ArrayList<>();

    // timer for benchmarking purposes
    ProcessTimer processTimer = new ProcessTimer();

    boolean benchMarkRun = false;

    resultStream = null;

    // Determine the data retrieval service for the server type used
    AbstractDataRetrievalService dataRetrievalService = determineDataRetrievalService();

    boolean generateCovidData =
        dataRetrievalService.getGlobalConfiguration().getGenerateCovidData()
            || isScopeActivatedViaParameters(scopes, COVID);

    boolean generateInfluenzaData =
        dataRetrievalService.getGlobalConfiguration().getGenerateInfluenzaData()
            || isScopeActivatedViaParameters(scopes, INFLUENZA);

    boolean generateKidsRadarData =
        dataRetrievalService.getGlobalConfiguration().getGenerateKidsRadarData()
            || isScopeActivatedViaParameters(scopes, KIDS_RADAR);

    boolean generateUkbRenalReplacementModelData =
        dataRetrievalService.getGlobalConfiguration().getGenerateUkbRenalReplacementModelData();

    // If custom codes are set in the yaml file -> update the default values.
    InputCodeSettings inputCodeSettings =
        ConfigurationTransformer.extractInputCodeSettings(dataRetrievalService);

    QualitativeLabCodesSettings qualitativeLabCodesSettings =
        ConfigurationTransformer.extractQualitativeLabCodesSettings(dataRetrievalService);

    try {
      // Starting with the calculation of the prediction models to release the memory right after.
      if (generateUkbRenalReplacementModelData) {
        Map<RenalReplacementRiskParameters, List<CoreBaseDataItem>>
            renalReplacementModelParameterSetMap =
                generateUkbModelData(
                    benchMarkRun,
                    dataRetrievalService,
                    processTimer,
                    globalConfiguration.getServerType());

        // Postprocessing
        if (renalReplacementModelParameterSetMap != null) {
          dataItems.addAll(
              RenalReplacementRiskDataGenerator.generateDataItems(
                  renalReplacementModelParameterSetMap));
        }
      }

      if (generateCovidData) {
        dataItems.addAll(
            CovidDataController.generateData(
                COVID,
                dataRetrievalService,
                reportConfiguration,
                processTimer,
                globalConfiguration,
                variantConfiguration,
                inputCodeSettings,
                qualitativeLabCodesSettings,
                exclDataItems,
                result));
        // End workflow if no resources were found
        if (LoggingHelper.gotWorkflowAborted(COVID)) {
          return new ResponseEntity<>(
              LoggingHelper.getAbortMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
      }

      if (generateInfluenzaData) {
        dataItems.addAll(
            InfluenzaDataController.generateData(
                INFLUENZA,
                dataRetrievalService,
                reportConfiguration,
                processTimer,
                globalConfiguration,
                variantConfiguration,
                inputCodeSettings,
                qualitativeLabCodesSettings,
                exclDataItems,
                result));
        // End workflow if no resources were found
        if (LoggingHelper.gotWorkflowAborted(INFLUENZA)) {
          return new ResponseEntity<>(
              LoggingHelper.getAbortMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
      }

      if (generateKidsRadarData) {
        dataItems.addAll(
            KidsRadarDataController.generateData(
                KIDS_RADAR,
                dataRetrievalService,
                reportConfiguration,
                processTimer,
                globalConfiguration,
                variantConfiguration,
                inputCodeSettings,
                qualitativeLabCodesSettings,
                exclDataItems,
                result));
        // End workflow if no resources were found
        if (LoggingHelper.gotWorkflowAborted(KIDS_RADAR)) {
          return new ResponseEntity<>(
              LoggingHelper.getAbortMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
      }

      ArrayNode dataItemsArrayNode = mapper.valueToTree(dataItems);
      result.putArray("dataitems").addAll(dataItemsArrayNode);
      result.put("provider", this.providerService.provConf.getName());
      result.put("dashboard_dataset_version", CURRENT_DATASET_VERSION);
      result.put("author", this.providerService.provConf.getAuthor());
      result.put("file_generated_by", FILE_GENERATOR);
      result.put("exporttimestamp", DateTools.getCurrentUnixTime());

      byte[] resultBuffer = result.toString().getBytes(StandardCharsets.UTF_8);
      this.resultSize = resultBuffer.length;
      this.resultStream = new ByteArrayInputStream(resultBuffer);

      return new ResponseEntity<>(result.toString(), HttpStatus.OK);
    } catch (SearchException ex) {
      // Log the error message with more information
      logger.error(WORKFLOW_ABORTED + ex.getMessage());
      return new ResponseEntity<>(
          "Error occurred while requesting data:\n\n " + ex.getMessage(),
          HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (HttpClientErrorException ex) {
      // Wrong credentials
      logger.error(WORKFLOW_ABORTED + ex.getMessage());
      return new ResponseEntity<>(
          "Connection to the FHIR server failed:\n\n " + ex.getMessage(), ex.getStatusCode());
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

  /**
   * Checks if a specific scope is activated via parameters, given a list of scopes and a data item
   * context.
   *
   * @param scopes a list of scope names, which may include mixed case strings
   * @param dataItemContext the context of the data item to check for activation
   * @return true if the specified data item context is activated by one of the scopes; false
   *     otherwise
   */
  public static boolean isScopeActivatedViaParameters(
      List<String> scopes, DataItemContext dataItemContext) {
    if (scopes == null) {
      return false;
    }

    // Convert all elements of the list to lowercase using Stream API
    List<String> lowerCaseScopes =
        scopes.stream()
            .map(String::toLowerCase) // Convert each element to lowercase
            .toList();

    // Use a switch expression for more concise code
    return switch (dataItemContext) {
      case COVID -> lowerCaseScopes.contains("covid");
      case INFLUENZA -> lowerCaseScopes.contains("influenza");
      case KIDS_RADAR -> lowerCaseScopes.contains("kiradar");
      // May be used in the future to split the kidsradar-context up
      case KIDS_RADAR_KJP -> lowerCaseScopes.contains("kjp");
      case KIDS_RADAR_RSV -> lowerCaseScopes.contains("rsv");
    };
  }

  private AbstractDataRetrievalService determineDataRetrievalService() {
    AbstractDataRetrievalService dataRetrievalService;
    if (globalConfiguration.getServerType() == ServerTypeEnum.ACUWAVE) {
      dataRetrievalService =
          new AcuwaveDataRetrievalService(
              acuwaveSearchService, this.acuwaveSearchConfiguration, this.globalConfiguration);
    } else {
      dataRetrievalService =
          new FhirDataRetrievalService(
              fhirSearchService, this.fhirSearchConfiguration, this.globalConfiguration);
    }
    return dataRetrievalService;
  }
}
