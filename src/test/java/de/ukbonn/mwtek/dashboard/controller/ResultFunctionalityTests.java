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

import static de.ukbonn.mwtek.dashboard.examples.InputCodeSettingsExampleData.getExampleData;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.ACRIBIS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.COVID;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.INFLUENZA;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_MAXTREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_TREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_MAXTREATMENTLEVEL;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import de.ukbonn.mwtek.dashboard.examples.GlobalConfigurationExamples;
import de.ukbonn.mwtek.dashboardlogic.AcribisDataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.BctDataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.DataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.KidsRadarDataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.misc.ResourceConverter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiConsent;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiQuestionnaireResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class ResultFunctionalityTests {

  private static final FhirContext FHIR_CTX = FhirContext.forR4();
  public static final String SAMPLE_FILE_INFLUENZA = "./samples/SampleBundle_Influenza.json";
  private static final List<MiiPatient> patients = new ArrayList<>();
  private static final List<MiiEncounter> encounters = new ArrayList<>();
  private static final List<MiiCondition> conditions = new ArrayList<>();
  private static final List<MiiObservation> observations = new ArrayList<>();
  private static final List<MiiProcedure> procedures = new ArrayList<>();
  private static final List<MiiLocation> locations = new ArrayList<>();
  private static final List<MiiConsent> consents = new ArrayList<>();
  private static final List<MiiQuestionnaireResponse> questionnaireResponses = new ArrayList<>();
  public static final String SAMPLE_FILE_COVID = "./samples/SampleBundle_Covid.json";
  public static final String SAMPLE_FILE_KIDS_RADAR = "./samples/SampleBundle_KiRadar.json";
  public static final String SAMPLE_FILE_ACRIBIS = "./samples/SampleBundle_Acribis.json";
  public static final String SAMPLE_FILE_BCT = "./samples/SampleBundle_Bct.json";
  static List<DiseaseDataItem> sampleData;

  @DisplayName("Getting data items for a run with at least one example patient.")
  @Test
  void testGeneratingDataItems() {
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());
  }

  @DisplayName("Getting data items for a run with at least one example patient.")
  static List<DiseaseDataItem> loadSampleData() throws IOException {
    List<DiseaseDataItem> output = new ArrayList<>();

    Map<String, List<DiseaseDataItem>> diseaseDataMap = new HashMap<>();

    // Parse all files and collect data
    for (String sampleFile :
        List.of(
            SAMPLE_FILE_COVID,
            SAMPLE_FILE_INFLUENZA,
            SAMPLE_FILE_KIDS_RADAR,
            SAMPLE_FILE_ACRIBIS,
            SAMPLE_FILE_BCT)) {
      parseSampleFile(sampleFile);

      // Determine the appropriate generator based on the file
      DataItemGenerator generator =
          switch (sampleFile) {
            case SAMPLE_FILE_KIDS_RADAR ->
                // Use the KidsRadarDataItemGenerator for KIDS_RADAR
                new KidsRadarDataItemGenerator(
                    conditions, observations, patients, encounters, procedures, locations);
            case SAMPLE_FILE_ACRIBIS ->
                // Use the AcribisDataItemGenerator for ACRIBIS
                new AcribisDataItemGenerator(
                    consents, conditions, patients, encounters, procedures, questionnaireResponses);
            case SAMPLE_FILE_BCT ->
                // Use the BctDataItemGenerator for BCT
                new BctDataItemGenerator(consents);
            default ->
                // Use the regular DataItemGenerator for COVID and INFLUENZA
                new DataItemGenerator(
                    conditions, observations, patients, encounters, procedures, locations);
          };

      // Generate and store data items in the map
      diseaseDataMap.put(
          sampleFile,
          generator.getDataItems(
              null,
              null,
              getExampleData(),
              null,
              determineContext(sampleFile),
              GlobalConfigurationExamples.getExampleSettings()));
    }

    // Merge all results into the final output list
    diseaseDataMap.values().forEach(output::addAll);
    return output;
  }

  protected static List<DiseaseDataItem> loadSampleData(DataItemContext context)
      throws IOException {
    String sampleFile =
        switch (context) {
          case COVID -> SAMPLE_FILE_COVID;
          case INFLUENZA -> SAMPLE_FILE_INFLUENZA;
          case KIDS_RADAR -> SAMPLE_FILE_KIDS_RADAR;
          case ACRIBIS -> SAMPLE_FILE_ACRIBIS;
          case BCT -> SAMPLE_FILE_BCT;
          default -> throw new IllegalArgumentException("Unsupported context: " + context);
        };

    parseSampleFile(sampleFile);

    DataItemGenerator generator =
        switch (context) {
          case KIDS_RADAR ->
              new KidsRadarDataItemGenerator(
                  conditions, observations, patients, encounters, procedures, locations);
          case ACRIBIS ->
              new AcribisDataItemGenerator(
                  consents, conditions, patients, encounters, procedures, questionnaireResponses);
          case BCT -> new BctDataItemGenerator(consents);
          default ->
              new DataItemGenerator(
                  conditions, observations, patients, encounters, procedures, locations);
        };

    return generator.getDataItems(
        null,
        null,
        getExampleData(),
        null,
        context,
        GlobalConfigurationExamples.getExampleSettings());
  }

  private static DataItemContext determineContext(String sampleFile) {
    return switch (sampleFile) {
      case SAMPLE_FILE_COVID -> COVID;
      case SAMPLE_FILE_INFLUENZA -> INFLUENZA;
      case SAMPLE_FILE_KIDS_RADAR -> KIDS_RADAR;
      case SAMPLE_FILE_ACRIBIS -> ACRIBIS;
      default -> throw new IllegalArgumentException("Unknown sample file: " + sampleFile);
    };
  }

  private static void parseSampleFile(String sampleFileCovid) throws IOException {
    parseJsonFile(sampleFileCovid);
  }

  public static void parseJsonFile(String jsonFile) throws IOException {
    try {
      // Clearing the resource lists before starting a new run to isolate the workflow data sources.
      clearResourceLists();
      File sampleFile = new File(jsonFile);
      String json = FileUtils.readFileToString(sampleFile, "UTF-8");

      // Use the FhirContext object to parse the JSON data into a Bundle
      Bundle bundle = (Bundle) FHIR_CTX.newJsonParser().parseResource(json);

      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        Resource resource = entry.getResource();

        // Check the type of the resource and convert it accordingly
        if (resource instanceof Encounter encounter) {
          encounters.add((MiiEncounter) ResourceConverter.convert(encounter));
        } else if (resource instanceof Patient patient) {
          patients.add((MiiPatient) ResourceConverter.convert(patient));
        } else if (resource instanceof Condition condition) {
          conditions.add((MiiCondition) ResourceConverter.convert(condition));
        } else if (resource instanceof Observation observation) {
          observations.add((MiiObservation) ResourceConverter.convert(observation));
        } else if (resource instanceof Procedure procedure) {
          procedures.add((MiiProcedure) ResourceConverter.convert(procedure));
        } else if (resource instanceof Location location) {
          locations.add((MiiLocation) ResourceConverter.convert(location));
        } else if (resource instanceof Consent consent) {
          consents.add((MiiConsent) ResourceConverter.convert(consent));
        } else if (resource instanceof QuestionnaireResponse questionnaireResponse) {
          questionnaireResponses.add(
              (MiiQuestionnaireResponse) ResourceConverter.convert(questionnaireResponse));
        }
      }
    } catch (IOException e) {
      throw new IOException("Unable to find the json fhir sample files", e.getCause());
    }
  }

  private static void clearResourceLists() {
    locations.clear();
    encounters.clear();
    patients.clear();
    observations.clear();
    procedures.clear();
    conditions.clear();
    consents.clear();
  }

  @Test
  @DisplayName("Testing data item exclusion")
  void testDataItemExclusion() throws IOException {
    Map<String, Boolean> mapExcludeDataItems = new HashMap<>();
    mapExcludeDataItems.put(CURRENT_TREATMENTLEVEL, true);
    mapExcludeDataItems.put(CURRENT_MAXTREATMENTLEVEL, true);
    mapExcludeDataItems.put(TIMELINE_MAXTREATMENTLEVEL, true);

    // First load the data items from the covid-19 sample file.
    parseSampleFile(SAMPLE_FILE_COVID);
    List<DiseaseDataItem> resultDataCovid =
        new DataItemGenerator(conditions, observations, patients, encounters, procedures, locations)
            .getDataItems(
                mapExcludeDataItems,
                null,
                getExampleData(),
                null,
                COVID,
                GlobalConfigurationExamples.getExampleSettings());

    assertAll(
        "Data items should be excluded",
        () ->
            assertTrue(
                resultDataCovid.stream()
                    .noneMatch(x -> x.getItemname().equals(CURRENT_TREATMENTLEVEL)),
                "CURRENT.TREATMENTLEVEL should be excluded"),
        () ->
            assertTrue(
                resultDataCovid.stream()
                    .noneMatch(x -> x.getItemname().equals(CURRENT_MAXTREATMENTLEVEL)),
                "CURRENT.MAXTREATMENTLEVEL should be excluded"),
        () ->
            assertTrue(
                resultDataCovid.stream()
                    .noneMatch(x -> x.getItemname().equals(TIMELINE_MAXTREATMENTLEVEL)),
                "TIMELINE.MAXTREATMENTLEVEL should be excluded"));
  }
}
