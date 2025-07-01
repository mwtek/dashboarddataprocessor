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

import static de.ukbonn.mwtek.dashboard.controller.DataRetrievalController.CURRENT_DDP_VERSION;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ukbonn.mwtek.dashboard.configuration.AcuwaveSearchConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.CustomGlobalConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.FhirSearchConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.FhirServerRestConfiguration;
import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.AcuwaveDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.AcuwaveSearchService;
import de.ukbonn.mwtek.dashboard.services.FhirDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.FhirSearchService;
import de.ukbonn.mwtek.dashboard.services.ProviderService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.HttpHostConnectException;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * REST controller for checking the status of the FHIR server connection.
 *
 * <p>This controller returns metadata about the configured FHIR server (e.g. version, name,
 * availability) and exposes general configuration details relevant for the dashboard operation.
 * Endpoint: <code>GET /status</code>
 */
@Slf4j
@Controller
@RequestMapping("/status")
public class StatusController {

  public static final String FHIR_SERVER = "fhir_server";
  // --- Constants for JSON keys ---
  public static final String FHIR_SERVER_CONNECTION_STATUS = FHIR_SERVER + "_connection_status";
  public static final String FHIR_SERVER_CONNECTION_STATUS_ERROR =
      FHIR_SERVER + "_connection_status_error";
  public static final String FHIR_SERVER_CONNECTION_STATUS_CODE =
      FHIR_SERVER + "_connection_status_code";
  public static final String FHIR_SERVER_NAME = FHIR_SERVER + "_name";
  public static final String FHIR_SERVER_VERSION = FHIR_SERVER + "_version";
  public static final String FHIR_SERVER_CONFIG_URL = FHIR_SERVER + "_config_url";
  public static final String FHIR_SERVER_CONFIG_AUTH_METHOD = FHIR_SERVER + "_config_auth_method";
  public static final String BATCH_SIZE = "batch_size";
  public static final String POST_METHOD_USED = "post_method_used";
  public static final String DDP_VERSION = "ddp_version";
  public static final String PROVIDER = "provider";

  private final AcuwaveSearchService acuwaveSearchService;
  private final FhirSearchService fhirSearchService;
  private final ProviderService providerService;

  @Autowired private CustomGlobalConfiguration customGlobalConfiguration;
  @Autowired private FhirSearchConfiguration fhirSearchConfiguration;
  @Autowired private FhirServerRestConfiguration fhirServerRestConfiguration;
  @Autowired private AcuwaveSearchConfiguration acuwaveSearchConfiguration;

  protected long resultSize = 0;
  protected InputStream resultStream = null;

  @Autowired
  public StatusController(
      AcuwaveSearchService acuwaveSearchService,
      FhirSearchService fhirSearchService,
      ProviderService providerService) {
    this.acuwaveSearchService = acuwaveSearchService;
    this.fhirSearchService = fhirSearchService;
    this.providerService = providerService;
  }

  @GetMapping
  public ResponseEntity<String> createStatusResponse(
      @RequestParam(required = false) List<String> scopes) {

    ObjectNode result = new ObjectMapper().createObjectNode();
    resultStream = null;

    AbstractDataRetrievalService dataRetrievalService = determineDataRetrievalService();
    HttpStatus status = HttpStatus.OK;

    try {
      CapabilityStatement cs = StatusDataController.generateData(dataRetrievalService);
      appendCapabilityStatementInfo(result, cs);
    } catch (Exception ex) {
      status = appendErrorInfo(result, ex);
    }

    appendConfigurationInfo(result);

    byte[] resultBuffer = result.toString().getBytes(StandardCharsets.UTF_8);
    this.resultSize = resultBuffer.length;
    this.resultStream = new ByteArrayInputStream(resultBuffer);

    return new ResponseEntity<>(result.toString(), status);
  }

  private AbstractDataRetrievalService determineDataRetrievalService() {
    return (customGlobalConfiguration.getServerType() == ServerTypeEnum.ACUWAVE)
        ? new AcuwaveDataRetrievalService(
            acuwaveSearchService, acuwaveSearchConfiguration, customGlobalConfiguration)
        : new FhirDataRetrievalService(
            fhirSearchService,
            fhirSearchConfiguration,
            customGlobalConfiguration,
            fhirServerRestConfiguration);
  }

  private void appendCapabilityStatementInfo(ObjectNode result, CapabilityStatement cs) {
    result.put(FHIR_SERVER_CONNECTION_STATUS, cs.getStatus().getDisplay());
    result.put(FHIR_SERVER_NAME, cs.getSoftware().getName());
    result.put(FHIR_SERVER_VERSION, cs.getSoftware().getVersion());
  }

  private HttpStatus appendErrorInfo(ObjectNode result, Exception ex) {
    String message;
    HttpStatus status;

    if (ex instanceof HttpHostConnectException || ex instanceof ResourceAccessException) {
      message = "FHIR server not reachable";
      result.put(FHIR_SERVER_CONNECTION_STATUS, "Inactive");
      result.put(FHIR_SERVER_CONNECTION_STATUS_ERROR, ex.getMessage());
      status = HttpStatus.SERVICE_UNAVAILABLE;

    } else if (ex instanceof HttpClientErrorException httpEx) {
      message = "FHIR server returned client error";
      result.put(FHIR_SERVER_CONNECTION_STATUS, "Inactive");
      result.put(FHIR_SERVER_CONNECTION_STATUS_CODE, httpEx.getStatusCode().toString());
      status = HttpStatus.SERVICE_UNAVAILABLE;

    } else {
      message = "Unexpected error during FHIR server check";
      result.put(FHIR_SERVER_CONNECTION_STATUS, "Inactive");
      result.put(FHIR_SERVER_CONNECTION_STATUS_ERROR, ex.getMessage());
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    log.error("Workflow aborted: {}", message, ex);
    return status;
  }

  private void appendConfigurationInfo(ObjectNode result) {
    result.put(PROVIDER, providerService.provConf.getName());
    result.put(FHIR_SERVER_CONFIG_URL, fhirServerRestConfiguration.getRestUrl());
    result.put(FHIR_SERVER_CONFIG_AUTH_METHOD, fhirServerRestConfiguration.getAuthMethod());
    result.put(BATCH_SIZE, fhirSearchConfiguration.getBatchSize());
    result.put(POST_METHOD_USED, fhirSearchConfiguration.isUsePostInsteadOfGet());
    result.put(DDP_VERSION, CURRENT_DDP_VERSION);
  }
}
