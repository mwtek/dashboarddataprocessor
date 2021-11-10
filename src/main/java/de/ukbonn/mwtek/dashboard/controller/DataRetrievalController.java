/*
 * Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 * modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 * PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 * OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import de.ukbonn.mwtek.dashboard.CoronaDashboardApplication;
import de.ukbonn.mwtek.dashboard.configuration.ExcludeDataItemsConfigurations;
import de.ukbonn.mwtek.dashboard.configuration.FhirSearchConfiguration;
import de.ukbonn.mwtek.dashboard.misc.ListHelper;
import de.ukbonn.mwtek.dashboard.misc.ResourceHandler;
import de.ukbonn.mwtek.dashboard.services.ProviderService;
import de.ukbonn.mwtek.dashboard.services.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ukbonn.mwtek.dashboardlogic.logic.CoronaLogic;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.CoronaResults;
import de.ukbonn.mwtek.utilities.fhir.misc.Converter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import de.ukbonn.mwtek.utilities.generic.time.DateTools;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller class for the "/createJson" REST endpoint with the main method to create the Json
 * specification for the Corona Dashboard.
 * 
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 *
 */
@Slf4j
@Controller
@RequestMapping("/createJson")
public class DataRetrievalController {

  Logger logger = LoggerFactory.getLogger(CoronaDashboardApplication.class);

  private SearchService searchSer;
  private ProviderService providerSer;
  @Autowired
  private ExcludeDataItemsConfigurations exclDataItems;
  @Autowired
  private FhirSearchConfiguration fhirSearchConfiguration;

  @Autowired
  public DataRetrievalController(SearchService searchService, ProviderService providerSer) {
    this.searchSer = searchService;
    this.providerSer = providerSer;
  }

  // Lists in which the retrieved FHIR resources are persisted. After conversion into UKB-FHIR
  // resources, these are discarded again.
  private Set<String> patientIds;
  private Set<String> encounterIds;
  private Set<Patient> setPatients;
  private Set<Encounter> setEncounter;
  private Set<Procedure> setProcedure;
  private Set<String> locationIds;
  private Set<Location> setLocation;

  /*
   * List with the entries of the bundles returned per FHIR search query
   */
  List<BundleEntryComponent> reqBundleEntry = null;
  /**
   * maximum number of IDs used at once as input filters in corresponding FHIR search queries (e.g.,
   * search for encounter resources by patientid). The recommended value here should be between 250
   * and 1000.
   */
  private int batchSize;
  /**
   * maximum count size per fhir request (e.g. if ask for encounter resources by patient IDs). This
   * value should never be reached.
   */
  private final int maxCountSize = 1000000;

  /**
   * the result size of the REST response
   */
  protected long resultSize = 0;
  protected InputStream resultStream = null;

  // timer for subroutines
  private long startTime;
  private long endTime;

  /**
   * REST endpoint (/createJson) that handles FHIR server data retrieval as well as processing in
   * Ukb resources, calling dashboard logic and providing Json output.
   * 
   * @return String with the json corona dashboard specification or alternatively an error message
   * @throws JsonProcessingException
   */
  @SuppressWarnings("unchecked")
  @GetMapping
  public ResponseEntity<String> createJson() throws JsonProcessingException {

    // initialize new request
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode result = mapper.createObjectNode();
    
    resultStream = null;

    // debug mode (extended output)
    Boolean debug = this.providerSer.provConf.getDebug();

    // setting batchSize
    this.batchSize = this.fhirSearchConfiguration.getBatchSize();

    // initialization of the individual FHIR resource sets and the plain id sets
    patientIds = new HashSet<>();
    encounterIds = new HashSet<>();
    setPatients = ConcurrentHashMap.newKeySet();
    setEncounter = ConcurrentHashMap.newKeySet();
    setProcedure = ConcurrentHashMap.newKeySet();
    locationIds = ConcurrentHashMap.newKeySet();
    setLocation = ConcurrentHashMap.newKeySet();

    // base fhir search queries
    String initialConditionQuery = "Condition?code="
        + fhirSearchConfiguration.getInputCodes().getOrDefault("condition", "U07.1!,U07.2!")
        + "&_getpageoffset=0&_pretty=false&_count=" + batchSize;

    String initialObservationQuery =
        "Observation?code=" + fhirSearchConfiguration.getInputCodes().getOrDefault("observation",
            "94640-0,94306-8,96763-8") + "&_getpageoffset=0&_pretty=false&_count=" + batchSize;
    String patientQuery = "Patient?_id=";
    String encounterQueryBase = "Encounter?subject=";
    String procedureQuery = "Procedure?category:coding=" + fhirSearchConfiguration.getInputCodes()
        .getOrDefault("procedure", "40617009,182744004,57485005") + "&subject=";
    String locationQuery = "Location?_id=";

    try {
      // Retrieval of the Observation resources
      logger.info("Retrieval of the Observation resources started");
      startTime = System.currentTimeMillis();
      Bundle initialObsBundle = this.searchSer.getInitialBundle(initialObservationQuery);
      List<Observation> listObservation = new ArrayList<>();
      int counterObs = 0;
      // FHIR servers normally deliver the data in bundles. Navigation is done via the link
      // attribute. "Self" contains the current query and "Next" the link to retrieve the following
      // bundle.
      while (initialObsBundle.hasLink() && initialObsBundle.getLink("next") != null) {
        // Parsing the retrieved resources and reading out the patients and Encounter Ids for later
        // data queries.
        ResourceHandler.handleObservationEntries(initialObsBundle, listObservation, patientIds,
            encounterIds);
        initialObsBundle = this.searchSer.getBundlePart(initialObsBundle.getLink("next").getUrl());

        logStatusDataRetrievalSequential(initialObsBundle.getTotal(), counterObs++,
            FHIRAllTypes.OBSERVATION.getDisplay());
      }
      // no next link? -> just add the results to the lists and go on
      ResourceHandler.handleObservationEntries(initialObsBundle, listObservation, patientIds,
          encounterIds);
      logger.info("Loading Observations took " + (System.currentTimeMillis() - startTime)
          + " milliseconds for " + listObservation.size() + " resources");

      List<UkbObservation> listUkbObservations =
          (List<UkbObservation>) Converter.convert(listObservation);

      // Release the origin list after converting from memory
      listObservation = null;

      // Retrieval of the Condition resources
      logger.info("Retrieval of the Condition resources started");
      startTime = System.currentTimeMillis();
      Bundle initialConditionBundle = this.searchSer.getInitialBundle(initialConditionQuery);
      List<Condition> listConditions = new ArrayList<>();
      int counterCond = 0;
      while (initialConditionBundle.hasLink() && initialConditionBundle.getLink("next") != null) {
        // Parsing the retrieved resources and reading out the patients and Encounter Ids for later
        // data queries.
        ResourceHandler.handleConditionEntries(initialConditionBundle, listConditions, patientIds,
            encounterIds);
        initialConditionBundle =
            this.searchSer.getBundlePart(initialConditionBundle.getLink("next").getUrl());

        logStatusDataRetrievalSequential(initialConditionBundle.getTotal(), counterCond++,
            FHIRAllTypes.CONDITION.getDisplay());
      }
      // no next link existent? -> just add the results to the lists and go further on
      ResourceHandler.handleConditionEntries(initialConditionBundle, listConditions, patientIds,
          encounterIds);
      endTime = System.currentTimeMillis();

      // map fhir resources into ukb resources
      List<UkbCondition> listUkbConditions = (List<UkbCondition>) Converter.convert(listConditions);
      logger.info("Loading Conditions took " + (endTime - startTime) + " milliseconds for "
          + listConditions.size() + " resources");

      // Release the origin list after converting from memory
      listConditions = null;

      // Retrieval of the Patient resources
      logger.info("Retrieval of the Patient resources started");
      startTime = System.currentTimeMillis();

      // put (unique) pids into a list for easier subsetting
      List<String> patientIdList = new ArrayList<>(patientIds);
      List<List<String>> patientIdSublists = ListHelper.splitList(patientIdList, batchSize);

      AtomicInteger counter = new AtomicInteger(0);
      patientIdSublists.forEach(patientSubList -> {
        String pidListString = StringUtils.join(patientSubList, ',');
        logStatusDataRetrievalParalelly(patientIdList.size(), counter.getAndIncrement(),
            FHIRAllTypes.PATIENT.getDisplay());

        reqBundleEntry =
            this.searchSer.getBundleData(patientQuery + pidListString + "&_count=" + batchSize);

        reqBundleEntry.parallelStream().forEach(singlePatient -> {
          setPatients.add((Patient) singlePatient.getResource());
        });
      });

      List<UkbPatient> listUkbPatients =
          (List<UkbPatient>) Converter.convert(new ArrayList<Patient>(setPatients));
      logger.info("Loading Patients took " + (System.currentTimeMillis() - startTime)
          + " milliseconds for " + setPatients.size() + " resources");

      // Release the origin set after converting from memory
      setPatients = null;

      // Retrieval of the Encounter resources
      logger.info("Retrieval of the Encounter resources started");
      startTime = System.currentTimeMillis();

      AtomicLong countProcessedEncounter = new AtomicLong(0);

      patientIdSublists.parallelStream().forEach(patientIdSublist -> {
        String patientIdListString = StringUtils.join(patientIdSublist, ',');

        StringBuilder encounterQuery = new StringBuilder(encounterQueryBase);

        // subject =
        encounterQuery.append(patientIdListString);

        // For this project, theoretically only cases with an intake date after a cut-off date
        // (28/01/2020) are needed. To reduce the resource results and make the queries more
        // streamlined, a "&location-period=gt2020-28-01" is added on demand to the fhir search, as
        // we cannot assume that every location stores the transfer history in the Encounter
        // resource.
        if (providerSer.provConf.getEncounterContainsLocationData())
          encounterQuery.append("&location-period=gt2020-01-28");

        // each patient can have 1:n encounters so the countsize in the fhir search differs from the
        // result size and since the result size is capped by default we need to increase the value
        // to get them all
        reqBundleEntry = this.searchSer
            .getBundleData(encounterQuery.append("&_count=" + maxCountSize).toString());

        reqBundleEntry.forEach(bundleEntry -> {
          Encounter encounter = (Encounter) bundleEntry.getResource();
          setEncounter.add(encounter);
          // add the unique location ids to a set to retrieve it later on
          encounter.getLocation().forEach(locEntry -> {
            locationIds.add(locEntry.getLocation().getReference().split("/")[1]);
          });
        });

        logStatusDataRetrievalParalelly(patientIdList.size(),
            countProcessedEncounter.getAndIncrement(), FHIRAllTypes.ENCOUNTER.getDisplay());
      });

      List<UkbEncounter> listUkbEncounters =
          (List<UkbEncounter>) Converter.convert(new ArrayList<Encounter>(setEncounter));

      logger.info("Loading Encounter took " + (System.currentTimeMillis() - startTime)
          + " milliseconds for " + setEncounter.size() + " resources");

      // Release the origin set after converting from memory
      setEncounter = null;

      // Retrieval of the Procedure resources
      logger.info("Retrieval of the Procedure resources started");
      startTime = System.currentTimeMillis();
      AtomicLong countProcessedProcedures = new AtomicLong(0);

      patientIdSublists.forEach(patientSubList -> {
        String patientIdListString = StringUtils.join(patientSubList, ',');

        reqBundleEntry = this.searchSer
            .getBundleData(procedureQuery + patientIdListString + "&_count=" + maxCountSize);

        reqBundleEntry.parallelStream().forEach(bundleEntry -> {
          Procedure procedure = (Procedure) bundleEntry.getResource();
          setProcedure.add(procedure);
        });

        logStatusDataRetrievalParalelly(patientIdList.size(),
            countProcessedProcedures.getAndIncrement(), FHIRAllTypes.PROCEDURE.getDisplay());
      });

      List<UkbProcedure> listUkbProcedures =
          (List<UkbProcedure>) Converter.convert(new ArrayList<Procedure>(setProcedure));
      logger.info("Loading Procedure took " + (System.currentTimeMillis() - startTime)
          + " milliseconds for " + setProcedure.size() + " resources");

      // Release the origin set after converting from memory
      setProcedure = null;

      // Retrieval of the Location resources
      logger.info("Retrieval of the Location resources started");
      startTime = System.currentTimeMillis();

      List<List<String>> locationIdSublists =
          ListHelper.splitList(new ArrayList<String>(locationIds), batchSize);

      locationIdSublists.forEach(locationIdSubList -> {
        String locationIdListString = StringUtils.join(locationIdSubList, ',');
        reqBundleEntry = this.searchSer
            .getBundleData(locationQuery + locationIdListString + "&_count=" + batchSize);

        reqBundleEntry.parallelStream().forEach(bundleEntry -> {
          Location location = (Location) bundleEntry.getResource();
          setLocation.add(location);
        });
      });

      List<UkbLocation> listUkbLocations = new ArrayList<>();
      listUkbLocations =
          (List<UkbLocation>) Converter.convert(new ArrayList<Location>(setLocation));
      logger.info("Loading Location took " + (System.currentTimeMillis() - startTime)
          + " milliseconds for " + setLocation.size() + " resources");

      // Release the origin set after converting from memory
      setLocation = null;

      // Start of the processing logic
      // Flagging of all c19 positive cases
      listUkbEncounters =
          CoronaLogic.flagCases(listUkbEncounters, listUkbConditions, listUkbObservations);

      // Formatting of resources in json specification
      CoronaResults coronaResults = new CoronaResults(listUkbConditions, listUkbObservations,
          listUkbPatients, listUkbEncounters, listUkbProcedures, listUkbLocations);

      // Creation of the data items of the dataset specification
      ArrayList<CoronaDataItem> dataItems =
          coronaResults.getDataItems(exclDataItems.getExcludes(), debug);
      ArrayNode array = mapper.valueToTree(dataItems);
      
      result.putArray("dataitems").addAll(array);
      result.put("provider", this.providerSer.provConf.getName());
      result.put("corona_dashboard_dataset_version", "0.1.3");
      result.put("author", this.providerSer.provConf.getAuthor());
      result.put("exporttimestamp", DateTools.getCurrentUnixTime());

      if (debug) {
        // put the sizes of each fhir resource list in the output
        result.put("conditionSize", listUkbConditions.size());
        result.put("observationSize", listUkbObservations.size());
        result.put("patientSize", listUkbPatients.size());
        result.put("encounterSize", listUkbEncounters.size());
        result.put("locationSize", listUkbLocations.size());
        result.put("procedureSize", listUkbProcedures.size());
      }

      byte[] resultBuffer = result.toString().getBytes(StandardCharsets.UTF_8);
      this.resultSize = resultBuffer.length;
      this.resultStream = new ByteArrayInputStream(resultBuffer);

      return new ResponseEntity<>(result.toString(), HttpStatus.OK);
    } catch (HttpClientErrorException ex) {
      logger.info("Workflow aborted: " + ex.getMessage());
      return new ResponseEntity<>("Connection to the FHIR server failed:\n\n " + ex.getMessage(),
          ex.getStatusCode());
    } catch (HttpServerErrorException ex) {
      logger.info("Workflow aborted: " + ex.getResponseBodyAsString());
      return new ResponseEntity<>(ex.getResponseBodyAsString(),
          HttpStatus.valueOf(ex.getRawStatusCode()));
    } catch (FHIRException ex) {
      logger.info("Workflow aborted: " + ex.getMessage());
      ex.printStackTrace();
      return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (Exception ex) {
      logger.info("Workflow aborted: " + ex.getMessage());
      logger.info("Workflow exception type: " + ex.getClass());
      logger.info("Workflow stack trace test: " + ex.getStackTrace());
      return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Providing status information about the current retrieval of FHIR resources for which the output
   * quantity is not determined before sending the FHIR Search Query (e.g. retrieval of all
   * Observation Resources with Loinc code 12345-6).
   * 
   * @param totalEntries Number of resources in the {@link Bundle}
   * @param counter Current counter of the processing resources (this value is not incremented in
   *          this method)
   * @param resourceType Resource type that is being processed
   */
  private void logStatusDataRetrievalSequential(int totalEntries, int counter,
      String resourceType) {
    if (counter > 0 && counter % 10 == 0)
      logger.info(
          "Retrieving " + resourceType + " data: " + counter * batchSize + "/" + totalEntries);
  }

  /**
   * Providing status information about the current retrieval of FHIR resources for which the output
   * quantity is determined before the FHIR search query is sent (e.g. retrieval of all patient
   * resources via supplied corresponding IDs) and where the queries can be parallelised.
   * 
   * @param inputParameterSize The total number of input IDs for which resources are retrieved
   * @param counter Current counter of the processing resources (this value is not incremented in
   *          this method)
   * @param resourceType Resource type that is being processed
   */
  private void logStatusDataRetrievalParalelly(int inputParameterSize, long counter,
      String resourceType) {
    if ((counter * batchSize) % 10000 == 0) {
      logger.info("Retrieving " + resourceType + " data for: " + counter * batchSize + "/"
          + inputParameterSize + " patients");
    }
  }
}
