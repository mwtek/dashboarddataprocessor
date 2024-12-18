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
import de.ukbonn.mwtek.dashboardlogic.DataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.KidsRadarDataItemGenerator;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.misc.ResourceConverter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ResultFunctionalityTests {

  public static final String SAMPLE_FILE_INFLUENZA = "./samples/SampleBundle_Influenza.json";
  private static final List<UkbPatient> ukbPatients = new ArrayList<>();
  private static final List<UkbEncounter> ukbEncounters = new ArrayList<>();
  private static final List<UkbCondition> ukbConditions = new ArrayList<>();
  private static final List<UkbObservation> ukbObservations = new ArrayList<>();
  private static final List<UkbProcedure> ukbProcedures = new ArrayList<>();
  private static final List<UkbLocation> ukbLocations = new ArrayList<>();
  public static final String SAMPLE_FILE_COVID = "./samples/SampleBundle_Covid.json";
  public static final String SAMPLE_FILE_KIDS_RADAR = "./samples/SampleBundle_KiRadar.json";
  static List<DiseaseDataItem> sampleData;

  @BeforeAll
  static void setupSampleData() throws IOException {
    sampleData = loadSampleData();
  }

  @DisplayName("Getting data items for a run with at least one example patient.")
  @Test
  void testGeneratingDataItems() {
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());
  }

  @DisplayName("Getting data items for a run with at least one example patient.")
  static List<DiseaseDataItem> loadSampleData() throws IOException {

    List<DiseaseDataItem> output = new ArrayList<>();

    // First load the data items from the covid-19 sample file.
    parseSampleFile(SAMPLE_FILE_COVID);
    List<DiseaseDataItem> resultDataCovid =
        new DataItemGenerator(
                ukbConditions,
                ukbObservations,
                ukbPatients,
                ukbEncounters,
                ukbProcedures,
                ukbLocations)
            .getDataItems(null, true, null, getExampleData(), null, COVID, true);

    // Then load the data items from the influenza sample file.
    parseSampleFile(SAMPLE_FILE_INFLUENZA);
    List<DiseaseDataItem> resultDataInfluenza =
        new DataItemGenerator(
                ukbConditions,
                ukbObservations,
                ukbPatients,
                ukbEncounters,
                ukbProcedures,
                ukbLocations)
            .getDataItems(null, true, null, getExampleData(), null, INFLUENZA, true);

    // Then load the data items from the influenza sample file.
    parseSampleFile(SAMPLE_FILE_KIDS_RADAR);
    List<DiseaseDataItem> resultDataKiRadar =
        new KidsRadarDataItemGenerator(
                ukbConditions,
                ukbObservations,
                ukbPatients,
                ukbEncounters,
                ukbProcedures,
                ukbLocations)
            .getDataItems(null, true, null, getExampleData(), null, KIDS_RADAR, true);

    output.addAll(resultDataCovid);
    output.addAll(resultDataInfluenza);
    output.addAll(resultDataKiRadar);
    return output;
  }

  private static void parseSampleFile(String sampleFileCovid) throws IOException {
    parseJsonFile(sampleFileCovid);
  }

  public static void parseJsonFile(String jsonFile) throws IOException {
    FhirContext ctx = FhirContext.forR4();
    try {
      // Clearing the resource lists before starting a new run to isolate the workflow data sources.
      clearResourceLists();
      File sampleFile = new File(jsonFile);
      String json = FileUtils.readFileToString(sampleFile, "UTF-8");

      // Use the FhirContext object to parse the JSON data into a Bundle
      Bundle bundle = (Bundle) ctx.newJsonParser().parseResource(json);

      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        Resource resource = entry.getResource();

        // Check the type of the resource and convert it accordingly
        if (resource instanceof Encounter encounter) {
          ukbEncounters.add((UkbEncounter) ResourceConverter.convert(encounter));
        } else if (resource instanceof Patient patient) {
          ukbPatients.add((UkbPatient) ResourceConverter.convert(patient));
        } else if (resource instanceof Condition condition) {
          ukbConditions.add((UkbCondition) ResourceConverter.convert(condition));
        } else if (resource instanceof Observation observation) {
          ukbObservations.add((UkbObservation) ResourceConverter.convert(observation));
        } else if (resource instanceof Procedure procedure) {
          ukbProcedures.add((UkbProcedure) ResourceConverter.convert(procedure));
        } else if (resource instanceof Location location) {
          ukbLocations.add((UkbLocation) ResourceConverter.convert(location));
        }
      }
    } catch (IOException e) {
      throw new IOException("Unable to find the json fhir sample files", e.getCause());
    }
  }

  private static void clearResourceLists() {
    ukbLocations.clear();
    ukbEncounters.clear();
    ukbPatients.clear();
    ukbObservations.clear();
    ukbProcedures.clear();
    ukbConditions.clear();
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
        new DataItemGenerator(
                ukbConditions,
                ukbObservations,
                ukbPatients,
                ukbEncounters,
                ukbProcedures,
                ukbLocations)
            .getDataItems(mapExcludeDataItems, true, null, getExampleData(), null, COVID, false);

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
