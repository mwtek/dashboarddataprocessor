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
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.RSV_ACUTE_BRONCHIOLITIS;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.RSV_ACUTE_BRONCHITIS;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.RSV_CAUSED_DISEASE;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.RSV_DIAGNOSES_ALL;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.RSV_PNEUMONIA;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.STRESS_RELATED_DISORDERS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR_KJP;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR_PED;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR_PED_RSV;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_AGE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_GENDER;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_DIAGS_ZIPCODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_TREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_ZIPCODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_DIAGS_OCCURRENCE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_MAXTREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.KiraAgePedCluster.AGE_0_3_M;
import static de.ukbonn.mwtek.dashboardlogic.enums.KiraAgePedCluster.AGE_1_2_Y;
import static de.ukbonn.mwtek.dashboardlogic.enums.KiraAgePedCluster.AGE_3_5_Y;
import static de.ukbonn.mwtek.dashboardlogic.enums.KiraAgePedCluster.AGE_6_12_Y;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.Pediatric.CPAP;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.Pediatric.ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.Pediatric.HIGHFLOW;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.Pediatric.INVASIVE_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.models.KiraInteger.THRESHOLD_UNEQUALITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.fail;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.StackedBarCharts;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.Pediatric;
import de.ukbonn.mwtek.dashboardlogic.models.ChartListItem;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.KiraInteger;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsUniformItem;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
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

  /** Load only KidsRadar sample data for this test class. */
  @BeforeAll
  void setupSampleData() throws IOException {
    // Calls the single-context helper provided by ResultFunctionalityTests
    sampleData = loadSampleData(KIDS_RADAR);
  }

  @Test
  @DisplayName("Testing kira.ped.rsv.cumulative.diags.zipcode")
  void testCumulativeDiagsZipCode() {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Test for RSV data
    verifyCumulativeDiagsZipCode(
        KIDS_RADAR_PED_RSV, RSV_DIAGNOSES_ALL, Arrays.asList("22549", "56789", "78953", "78953"));
    verifyCumulativeDiagsZipCode(
        KIDS_RADAR_PED_RSV,
        RSV_ACUTE_BRONCHIOLITIS,
        Arrays.asList("22549", "56789", "78953", "78953"));
    verifyCumulativeDiagsZipCode(KIDS_RADAR_PED_RSV, RSV_PNEUMONIA, List.of("56789"));
  }

  @Test
  @DisplayName("Testing kira.ped.current.zipcodes")
  void testCurrentZipCodes() {
    assertListExactly(KIDS_RADAR_PED, CURRENT_ZIPCODE, List.of("22549", "22549", "78953"));
    assertListIsSortedAscending(KIDS_RADAR_PED, CURRENT_ZIPCODE);
  }

  @Test
  @DisplayName("Testing kira.ped.current.treatmentlevel")
  @Override
  void testCurrentTreatmentLevel() {
    assertTreatmentLevelKiradarPed(KIDS_RADAR_PED, CURRENT_TREATMENTLEVEL, 0, 0, 0, 1, 1, 1);
  }

  @Test
  @DisplayName("Testing cumulative diagnostics by zip code for KJP and RSV")
  void testCumulativeDiagsGender() {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    verifyCumulativeDiagsGender(
        KIDS_RADAR_KJP, STRESS_RELATED_DISORDERS, Arrays.asList(1, 1, THRESHOLD_UNEQUALITY));
    verifyCumulativeDiagsGender(
        KIDS_RADAR_KJP,
        OTHER_PSYCHOLOGICAL_DISORDERS,
        Arrays.asList(4, THRESHOLD_UNEQUALITY, THRESHOLD_UNEQUALITY));

    // Test for RSV data
    verifyCumulativeDiagsGender(
        KIDS_RADAR_PED_RSV, RSV_ACUTE_BRONCHIOLITIS, Arrays.asList(1, 4, 0));
    verifyCumulativeDiagsGender(KIDS_RADAR_PED_RSV, RSV_CAUSED_DISEASE, Arrays.asList(0, 0, 0));
    verifyCumulativeDiagsGender(KIDS_RADAR_PED_RSV, RSV_PNEUMONIA, Arrays.asList(1, 0, 0));
  }

  @Test
  @DisplayName("Testing kira.ped.rsv.cumulative.diags.age")
  void testCumulativeDiagsAgeRsv() {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Labeling the indices of the output
    Map<String, Integer> rsvExpectedIndices =
        Map.of(
            RSV_ACUTE_BRONCHIOLITIS, RSV_IDX_BRONCHIOLITIS,
            RSV_CAUSED_DISEASE, RSV_IDX_CAUSED_DISEASE,
            RSV_ACUTE_BRONCHITIS, RSV_IDX_BRONCHITIS,
            RSV_PNEUMONIA, RSV_IDX_PNEUMONIA);

    // Test for RSV data
    verifyCumulativeDiagsAge(
        KIDS_RADAR_PED_RSV,
        null,
        AGE_0_3_M.getLabel(),
        Arrays.asList(1, 0, 0, 1),
        rsvExpectedIndices);
    verifyCumulativeDiagsAge(
        KIDS_RADAR_PED_RSV,
        null,
        AGE_1_2_Y.getLabel(),
        Arrays.asList(3, 0, 0, 0),
        rsvExpectedIndices);
    verifyCumulativeDiagsAge(
        KIDS_RADAR_PED_RSV,
        null,
        AGE_3_5_Y.getLabel(),
        Arrays.asList(0, 0, 0, 0),
        rsvExpectedIndices);
  }

  // TODO Activate KJP data again
  @DisplayName("Testing cumulative age charts for KJP and RSV")
  void testCumulativeDiagsAgeKjp() {
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
        AGE_6_12_Y.getLabel(),
        Arrays.asList(1, 1),
        kjpExpectedIndices);
    // Expecting [1,1] for 'age_kjp_6-8y' for stress related disorders
    verifyCumulativeDiagsAge(
        KIDS_RADAR_KJP,
        STRESS_RELATED_DISORDERS,
        AGE_6_12_Y.getLabel(),
        Arrays.asList(1, 1),
        kjpExpectedIndices);

    // Test for RSV data
    verifyCumulativeDiagsAge(
        KIDS_RADAR_PED_RSV,
        null,
        AGE_0_3_M.getLabel(),
        Arrays.asList(1, 0, 0, 1),
        rsvExpectedIndices);
    verifyCumulativeDiagsAge(
        KIDS_RADAR_PED_RSV,
        null,
        AGE_6_12_Y.getLabel(),
        Arrays.asList(1, 0, 0, 0),
        rsvExpectedIndices);
  }

  @Test
  @DisplayName("Testing cumulative length of stay charts for KJP and RSV")
  void testCumulativeLengthOfStay() {

    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    verifyCumulativeLengthOfStay(KIDS_RADAR_KJP, OTHER_PSYCHOLOGICAL_DISORDERS, List.of(60.5));
    verifyCumulativeLengthOfStay(KIDS_RADAR_KJP, STRESS_RELATED_DISORDERS, List.of(112.0));

    // Test for RSV data
    verifyCumulativeLengthOfStay(KIDS_RADAR_PED_RSV, RSV_CAUSED_DISEASE, List.of(0.0));
    verifyCumulativeLengthOfStay(KIDS_RADAR_PED_RSV, RSV_PNEUMONIA, List.of(17.0));
  }

  @Test
  @DisplayName("Testing timeline diags charts for KJP and RSV")
  void testTimelineDiagsOccurrence() {

    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Test for KJP data
    verifyTimelineDiagsOccurrence(KIDS_RADAR_KJP, "2021-06", Arrays.asList(3, 0));
    verifyTimelineDiagsOccurrence(KIDS_RADAR_KJP, "2024-01", Arrays.asList(1, 1));
    verifyTimelineDiagsOccurrence(KIDS_RADAR_KJP, "2024-02", Arrays.asList(0, 0));

    // Test for RSV data
    verifyTimelineDiagsOccurrence(KIDS_RADAR_PED_RSV, "2022-01", Arrays.asList(0, 0, 0, 0));
    verifyTimelineDiagsOccurrence(KIDS_RADAR_PED_RSV, "2022-02", Arrays.asList(1, 0, 0, 1));
    verifyTimelineDiagsOccurrence(KIDS_RADAR_PED_RSV, "2023-02", Arrays.asList(1, 0, 0, 0));
    verifyTimelineDiagsOccurrence(KIDS_RADAR_PED_RSV, "2025-07", Arrays.asList(1, 0, 0, 0));
  }

  @Test
  @DisplayName("Testing kira.ped.timeline.maxtreatmentlevel")
  void testTimelineMaxTreatmentlevel() {
    assertTimelineValueByDay(
        KIDS_RADAR_PED, TIMELINE_MAXTREATMENTLEVEL, 1625011200L, 3, Pediatric.NORMAL_WARD);
    assertTimelineValueByDay(
        KIDS_RADAR_PED, TIMELINE_MAXTREATMENTLEVEL, 1758585600L, 0, Pediatric.NORMAL_WARD);

    assertTimelineValueByDay(
        KIDS_RADAR_PED, TIMELINE_MAXTREATMENTLEVEL, 1625011200L, 0, Pediatric.ICU);
    assertTimelineValueByDay(
        KIDS_RADAR_PED, TIMELINE_MAXTREATMENTLEVEL, 1758585600L, 0, Pediatric.ICU);

    assertTimelineValueByDay(KIDS_RADAR_PED, TIMELINE_MAXTREATMENTLEVEL, 1625011200L, 0, CPAP);
    assertTimelineValueByDay(KIDS_RADAR_PED, TIMELINE_MAXTREATMENTLEVEL, 1758585600L, 1, CPAP);

    assertTimelineValueByDay(KIDS_RADAR_PED, TIMELINE_MAXTREATMENTLEVEL, 1625011200L, 0, HIGHFLOW);
    assertTimelineValueByDay(KIDS_RADAR_PED, TIMELINE_MAXTREATMENTLEVEL, 1758585600L, 1, HIGHFLOW);

    assertTimelineValueByDay(
        KIDS_RADAR_PED, TIMELINE_MAXTREATMENTLEVEL, 1625011200L, 0, INVASIVE_VENTILATION);
    assertTimelineValueByDay(
        KIDS_RADAR_PED, TIMELINE_MAXTREATMENTLEVEL, 1758585600L, 1, INVASIVE_VENTILATION);

    assertTimelineValueByDay(KIDS_RADAR_PED, TIMELINE_MAXTREATMENTLEVEL, 1625011200L, 0, ECMO);
    assertTimelineValueByDay(KIDS_RADAR_PED, TIMELINE_MAXTREATMENTLEVEL, 1758585600L, 0, ECMO);
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
      DataItemContext dataItemContext, String chartType, List<?> expectedValues) {

    // KJP and PED got different data type since one uses anonymization and the other not
    Function<Object, Integer> toInt =
        v -> {
          if (v instanceof KiraInteger(Number number)) return number.intValue();
          if (v instanceof Number n) return n.intValue();
          throw new IllegalArgumentException(
              "Unsupported value type: " + (v == null ? "null" : v.getClass()));
        };

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(dataItemContext, CUMULATIVE_DIAGS_GENDER);
    assertNotNull(dataItem, "DataItem should not be null");

    // Accept either StackedBarChartsItem<KiraValue> or StackedBarChartsItem<Integer>
    StackedBarChartsItem<?> result = (StackedBarChartsItem<?>) dataItem.getData();
    assertNotNull(result, "StackedBarChartsItem result should not be null");

    // Ensure charts are present
    List<String> charts = result.getCharts();
    assertNotNull(charts, "Charts list should not be null");
    assertFalse(charts.isEmpty(), "Charts list should not be empty");

    // Fetch values for the specified bar (chartType)
    List<?> valueTriplet = result.getValueByBarChart(chartType);
    assertNotNull(valueTriplet, "Values for chart " + chartType + " should not be null");

    // Assert size matches
    assertEquals(
        expectedValues.size(), valueTriplet.size(), "Expected value triplet size for " + chartType);

    // Compare number and/or string
    int maleActual = toInt.apply(valueTriplet.get(INDEX_MALE));
    Object maleExp = expectedValues.get(INDEX_MALE);
    assertTrue(
        matchesExpected(maleActual, maleExp),
        "Expected 'male' " + maleExp + " but was " + maleActual + " for chart type " + chartType);

    int femaleActual = toInt.apply(valueTriplet.get(INDEX_FEMALE));
    Object femaleExp = expectedValues.get(INDEX_FEMALE);
    assertTrue(
        matchesExpected(femaleActual, femaleExp),
        "Expected 'female' "
            + femaleExp
            + " but was "
            + femaleActual
            + " for chart type "
            + chartType);

    int diverseActual = toInt.apply(valueTriplet.get(INDEX_DIVERSE));
    Object diverseExp = expectedValues.get(INDEX_DIVERSE);
    assertTrue(
        matchesExpected(diverseActual, diverseExp),
        "Expected 'diverse' "
            + diverseExp
            + " but was "
            + diverseActual
            + " for chart type "
            + chartType);
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
    for (Entry<String, Integer> entry : expectedIndices.entrySet()) {
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
        expectedValues.getFirst().doubleValue(),
        value.getFirst().doubleValue(),
        "Expected 'length of stay' values does not match for bar type " + barType);

    // Validate that all results just got 1 decimal place as maximum
    assertTrue(
        value.stream().map(Number::doubleValue).allMatch(v -> BigDecimal.valueOf(v).scale() <= 1),
        "All values for bar type " + barType + " must have at most one decimal place");
  }

  private void verifyTimelineDiagsOccurrence(
      DataItemContext dataItemContext, String barType, List<? extends Number> expectedValues) {

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(dataItemContext, TIMELINE_DIAGS_OCCURRENCE);
    assertNotNull(dataItem, "DataItem should not be null");
    Object rawData = dataItem.getData();
    assertNotNull(rawData, "StackedBarCharts result should not be null");

    StackedBarCharts<?> result;
    if (rawData instanceof StackedBarChartsItem<?> stackedBarChartsItem) {
      result = stackedBarChartsItem;
    } else if (rawData instanceof StackedBarChartsUniformItem<?> stackedBarChartsUniformItem) {
      result = stackedBarChartsUniformItem;
    } else {
      fail(
          "Unexpected data type: "
              + rawData.getClass().getSimpleName()
              + ". Expected StackedBarChartsItem or StackedBarChartsUniformItem.");
      return;
    }

    assertNotNull(result, "StackedBarChartsItem result should not be null");

    // Ensure charts are present
    List<String> charts = result.getCharts();
    assertNotNull(charts, "Charts list should not be null");
    assertFalse(charts.isEmpty(), "Charts list should not be empty");

    // Elements with size are expected
    List<?> value = result.getValueByBarChart(barType);
    Object firstValue = value.getFirst();

    double actualValue =
        switch (firstValue) {
          case KiraInteger kiraInteger -> kiraInteger.number().doubleValue();
          case Number number -> number.doubleValue();
          default -> throw new IllegalStateException("Unexpected value: " + firstValue);
        };

    assertEquals(
        expectedValues.getFirst().doubleValue(),
        actualValue,
        "Expected 'timeline' values does not match for bar type " + barType);
  }

  @Override
  void testCurrentMaxTreatmentLevel() {}

  @Override
  void testCumulativeResults() {}

  /** Matches either an exact numeric expectation or the "<=3" placeholder */
  private static boolean matchesExpected(int actual, Object expected) {
    if (expected instanceof Number n) {
      return actual == n.intValue();
    }
    if (expected instanceof String s) {
      return s.trim().equals(THRESHOLD_UNEQUALITY) && actual <= KiraInteger.THRESHOLD;
    }
    throw new IllegalArgumentException(
        "Unsupported expected value type: " + (expected == null ? "null" : expected.getClass()));
  }
}
