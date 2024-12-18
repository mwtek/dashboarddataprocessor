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

import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.OTHER_PSYCHOLOGICAL_DISORDERS;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.STRESS_RELATED_DISORDERS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR_KJP;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR_RSV;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_AGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_GENDER;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_ZIPCODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_DIAGS_OCCURRENCE;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.RSV_ACUTE_BRONCHIOLITIS;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.RSV_CAUSED_DISEASE;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.RSV_DIAGNOSES_ALL;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarConstants.RSV_PNEUMONIA;
import static de.ukbonn.mwtek.dashboardlogic.enums.KiraAgeKjpCluster.AGE_6_8;
import static de.ukbonn.mwtek.dashboardlogic.enums.KiraAgeRsvCluster.AGE_0_3_M;
import static de.ukbonn.mwtek.dashboardlogic.enums.KiraAgeRsvCluster.AGE_6_17_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.ChartListItem;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class KidsRadarDataItemTests extends DataItemTests {

  public static final int INDEX_MALE = 0;
  public static final int INDEX_FEMALE = 1;
  public static final int INDEX_DIVERSE = 2;

  public static final int IN_GROUP = 0;
  public static final int OUT_GROUP = 0;

  public static final int RSV_IDX_BRONCHIOLITIS = 0;
  public static final int RSV_IDX_CAUSED_DISEASE = 1;
  public static final int RSV_IDX_BRONCHITIS = 2;
  public static final int RSV_IDX_PNEUMONIA = 3;

  @Test
  @DisplayName("Testing cumulative diagnostics by zip code for KJP and RSV")
  void testCumulativeDiagsZipCode() {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Test for KJP data
    verifyCumulativeDiagsZipCode(
        KIDS_RADAR_KJP, OTHER_PSYCHOLOGICAL_DISORDERS, Arrays.asList("123", "456"));
    verifyCumulativeDiagsZipCode(
        KIDS_RADAR_KJP, STRESS_RELATED_DISORDERS, Arrays.asList("123", "789"));

    // Test for RSV data
    verifyCumulativeDiagsZipCode(KIDS_RADAR_RSV, RSV_DIAGNOSES_ALL, Arrays.asList("567", "789"));
  }

  @Test
  @DisplayName("Testing cumulative diagnostics by zip code for KJP and RSV")
  void testCumulativeDiagsGender() {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Test for KJP data
    verifyCumulativeDiagsGender(
        KIDS_RADAR_KJP, OTHER_PSYCHOLOGICAL_DISORDERS, Arrays.asList(2, 0, 0));
    verifyCumulativeDiagsGender(KIDS_RADAR_KJP, STRESS_RELATED_DISORDERS, Arrays.asList(1, 1, 0));

    // Test for RSV data
    verifyCumulativeDiagsGender(KIDS_RADAR_RSV, RSV_ACUTE_BRONCHIOLITIS, Arrays.asList(1, 1, 0));
    //    verifyCumulativeDiagsGender(KIDS_RADAR_RSV, RSV_ACUTE_BRONCHITIS, Arrays.asList(0, 0, 0));
    verifyCumulativeDiagsGender(KIDS_RADAR_RSV, RSV_PNEUMONIA, Arrays.asList(1, 0, 0));
  }

  @Test
  @DisplayName("Testing cumulative age charts for KJP and RSV")
  void testCumulativeDiagsAge() {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Labeling the indices of the output
    Map<String, Integer> kjpExpectedIndices = Map.of("IN_GROUP", IN_GROUP, "OUT_GROUP", OUT_GROUP);
    Map<String, Integer> rsvExpectedIndices =
        Map.of(
            "BRONCHIOLITIS", RSV_IDX_BRONCHIOLITIS,
            "CAUSED_DISEASE", RSV_IDX_CAUSED_DISEASE,
            "BRONCHITIS", RSV_IDX_BRONCHITIS,
            "PNEUMONIA", RSV_IDX_PNEUMONIA);

    // Test for KJP data
    verifyCumulativeDiagsAge(
        KIDS_RADAR_KJP,
        OTHER_PSYCHOLOGICAL_DISORDERS,
        AGE_6_8.getLabel(),
        Arrays.asList(1, 1),
        kjpExpectedIndices);
    // Expecting [1,1] for 'age_kjp_6-8y' for stress related disorders
    verifyCumulativeDiagsAge(
        KIDS_RADAR_KJP,
        STRESS_RELATED_DISORDERS,
        AGE_6_8.getLabel(),
        Arrays.asList(1, 1),
        kjpExpectedIndices);

    // Test for RSV data
    verifyCumulativeDiagsAge(
        KIDS_RADAR_RSV, null, AGE_0_3_M.getLabel(), Arrays.asList(1, 0, 0, 1), rsvExpectedIndices);
    verifyCumulativeDiagsAge(
        KIDS_RADAR_RSV, null, AGE_6_17_Y.getLabel(), Arrays.asList(1, 0, 0, 0), rsvExpectedIndices);
  }

  @Test
  @DisplayName("Testing cumulative length of stay charts for KJP and RSV")
  void testCumulativeLengthOfStay() {

    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Test for KJP data
    verifyCumulativeLengthOfStay(KIDS_RADAR_KJP, OTHER_PSYCHOLOGICAL_DISORDERS, List.of(119.0));
    verifyCumulativeLengthOfStay(KIDS_RADAR_KJP, STRESS_RELATED_DISORDERS, List.of(112.0));

    // Test for RSV data
    verifyCumulativeLengthOfStay(KIDS_RADAR_RSV, RSV_ACUTE_BRONCHIOLITIS, List.of(17.0));
    verifyCumulativeLengthOfStay(KIDS_RADAR_RSV, RSV_CAUSED_DISEASE, List.of(0.0));
    verifyCumulativeLengthOfStay(KIDS_RADAR_RSV, RSV_PNEUMONIA, List.of(17.0));
  }

  @Test
  @DisplayName("Testing timeline charts for KJP and RSV")
  void testTimelineDiagsOccurrence() {

    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Test for KJP data
    verifyTimelineDiagsOccurrence(KIDS_RADAR_KJP, "2021-06", Arrays.asList(1, 0));
    verifyTimelineDiagsOccurrence(KIDS_RADAR_KJP, "2024-01", Arrays.asList(1, 1));
    verifyTimelineDiagsOccurrence(KIDS_RADAR_KJP, "2024-02", Arrays.asList(0, 0));

    // Test for RSV data
    verifyTimelineDiagsOccurrence(KIDS_RADAR_RSV, "2022-01", Arrays.asList(0, 0, 0, 0));
    verifyTimelineDiagsOccurrence(KIDS_RADAR_RSV, "2022-02", Arrays.asList(1, 0, 0, 1));
    verifyTimelineDiagsOccurrence(KIDS_RADAR_RSV, "2023-02", Arrays.asList(1, 0, 0, 0));
  }

  // Helper method to verify cumulative diagnostics by zip code for different data contexts
  private void verifyCumulativeDiagsZipCode(
      DataItemContext context, String chartType, List<String> expectedValues) {
    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(context, CUMULATIVE_DIAGS_ZIPCODE);
    assertNotNull(dataItem, "DataItem should not be null");

    ChartListItem result = (ChartListItem) dataItem.getData();
    assertNotNull(result, "ChartListItem result should not be null");

    // Ensure charts are present
    List<String> charts = result.getCharts();
    assertNotNull(charts, "Charts list should not be null");
    assertFalse(charts.isEmpty(), "Charts list should not be empty");

    // Check values for the specified chart type
    List<String> values = result.getValueByChart(chartType);
    assertNotNull(values, "Values for chart " + chartType + " should not be null");

    // Assert that the list size matches the expected values
    assertEquals(expectedValues.size(), values.size(), "Expected list size for " + chartType);
    assertIterableEquals(
        expectedValues, values, "ZIP-Codes for " + chartType + " should match the expected values");
  }

  private void verifyCumulativeDiagsGender(
      DataItemContext dataItemContext, String chartType, List<? extends Number> expectedValues) {

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(dataItemContext, CUMULATIVE_DIAGS_GENDER);
    assertNotNull(dataItem, "DataItem should not be null");

    StackedBarChartsItem result = (StackedBarChartsItem) dataItem.getData();
    assertNotNull(result, "StackedBarChartsItem result should not be null");

    // Ensure charts are present
    List<String> charts = result.getCharts();
    assertNotNull(charts, "Charts list should not be null");
    assertFalse(charts.isEmpty(), "Charts list should not be empty");

    // Check valueTriplet for the specified chart type
    List<? extends Number> valueTriplet = result.getValueByBarChart(chartType);
    assertNotNull(valueTriplet, "Values for chart " + chartType + " should not be null");

    // Assert that the list size matches the expected valueTriplet size
    assertEquals(
        expectedValues.size(), valueTriplet.size(), "Expected value triplet size for " + chartType);

    // Validate individual fields: male, female, diverse
    assertEquals(
        expectedValues.get(INDEX_MALE).intValue(),
        valueTriplet.get(INDEX_MALE).intValue(),
        "Expected 'male' value does not match for chart type " + chartType);

    assertEquals(
        expectedValues.get(INDEX_FEMALE).intValue(),
        valueTriplet.get(INDEX_FEMALE).intValue(),
        "Expected 'female' value does not match for chart type " + chartType);

    assertEquals(
        expectedValues.get(INDEX_DIVERSE).intValue(),
        valueTriplet.get(INDEX_DIVERSE).intValue(),
        "Expected 'diverse' value does not match for chart type " + chartType);
  }

  private void verifyCumulativeDiagsAge(
      DataItemContext dataItemContext,
      String chartType,
      String barType,
      List<? extends Number> expectedValues,
      Map<String, Integer> expectedIndices) {

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(dataItemContext, CUMULATIVE_DIAGS_AGE);
    assertNotNull(dataItem, "DataItem should not be null");

    StackedBarChartsItem result = (StackedBarChartsItem) dataItem.getData();
    assertNotNull(result, "StackedBarChartsItem result should not be null");

    // Ensure that charts and bars are present based on type
    List<String> charts = result.getCharts();
    List<List<String>> bars = result.getBars();
    assertTrue(
        charts != null && !charts.isEmpty() || bars != null && !bars.isEmpty(),
        "Charts or Bars list should not be null or empty");

    // Get the appropriate value pairs for the specified bar type
    List<? extends Number> valuePairs = result.getValueByBarChart(barType);
    assertNotNull(valuePairs, String.format("Values for bar '%s' should not be null", barType));

    // Assert list size matches expected values
    assertEquals(
        expectedValues.size(),
        valuePairs.size(),
        String.format("Expected value pair size for '%s' does not match", barType));

    // Validate individual fields based on expected indices
    for (Map.Entry<String, Integer> entry : expectedIndices.entrySet()) {
      String key = entry.getKey();
      int index = entry.getValue();
      assertEquals(
          expectedValues.get(index).intValue(),
          valuePairs.get(index).intValue(),
          String.format("Expected '%s' value does not match for bar type '%s'", key, barType));
    }
  }

  private void verifyCumulativeLengthOfStay(
      DataItemContext dataItemContext, String barType, List<? extends Number> expectedValues) {

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(dataItemContext, CUMULATIVE_LENGTHOFSTAY);
    assertNotNull(dataItem, "DataItem should not be null");

    StackedBarChartsItem result = (StackedBarChartsItem) dataItem.getData();
    assertNotNull(result, "StackedBarChartsItem result should not be null");

    // Ensure charts are present
    List<String> charts = result.getCharts();
    assertNotNull(charts, "Charts list should not be null");
    assertFalse(charts.isEmpty(), "Charts list should not be empty");

    // Elements with size are expected
    List<? extends Number> value = result.getValueByBarChart(barType);
    assertNotNull(value, String.format("Values for bar '%s' should not be null", barType));

    // Assert that the list size matches the expected valueTriplet size
    assertEquals(expectedValues.size(), value.size(), "Expected value pair size for " + barType);

    // Validate if the expected value is the same as the given value
    assertEquals(
        expectedValues.get(0).doubleValue(),
        value.get(0).doubleValue(),
        "Expected 'length of stay' values does not match for bar type " + barType);
  }

  private void verifyTimelineDiagsOccurrence(
      DataItemContext dataItemContext, String barType, List<? extends Number> expectedValues) {

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(dataItemContext, TIMELINE_DIAGS_OCCURRENCE);
    assertNotNull(dataItem, "DataItem should not be null");

    StackedBarChartsItem result = (StackedBarChartsItem) dataItem.getData();
    assertNotNull(result, "StackedBarChartsItem result should not be null");

    // Ensure charts are present
    List<String> charts = result.getCharts();
    assertNotNull(charts, "Charts list should not be null");
    assertFalse(charts.isEmpty(), "Charts list should not be empty");

    // Elements with size are expected
    List<? extends Number> value = result.getValueByBarChart(barType);
    assertNotNull(value, String.format("Values for bar '%s' should not be null", barType));

    // Assert that the list size matches the expected values size
    assertEquals(expectedValues.size(), value.size(), "Expected values size for " + barType);

    // Validate if the expected value is the same as the given value
    assertEquals(
        expectedValues.get(0).doubleValue(),
        value.get(0).doubleValue(),
        "Expected 'timeline' values does not match for bar type " + barType);
  }

  @Override
  void testCurrentTreatmentLevel() {}

  @Override
  void testCurrentMaxTreatmentLevel() {}

  @Override
  void testCumulativeResults() {}
}
