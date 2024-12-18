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

import static de.ukbonn.mwtek.dashboardlogic.DataItemGenerator.determineLabel;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.BORDERLINE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.DATE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.DIVERSE_SPECIFICATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.FEMALE_SPECIFICATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.MALE_SPECIFICATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.NEGATIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.POSITIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.OUTPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.tools.TimelineTools.getIndexByTimestamp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.TimestampedListPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public abstract class DataItemTests extends ResultFunctionalityTests {

  protected DiseaseDataItem getTreatmentDataItem(DataItemContext context, String dataItem) {
    return sampleData.stream()
        .filter(x -> x.getItemname().equals(determineLabel(context, dataItem)))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Data item not found for context: " + context));
  }

  /**
   * Asserts the treatment level data for a specific context.
   *
   * @param context The data item context.
   * @param normalWard The expected value for normal ward treatment.
   * @param icu The expected value for ICU treatment.
   * @param icuVentilation The expected value for ICU ventilation treatment.
   * @param icuEcmo The expected value for ICU ECMO treatment.
   */
  protected void assertTreatmentLevel(
      DataItemContext context,
      String selectedDataItem,
      Integer outpatient,
      Integer normalWard,
      Integer icu,
      Integer icuVentilation,
      Integer icuEcmo) {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(context, selectedDataItem);

    Map<String, Number> result = (Map<String, Number>) dataItem.getData();

    // Perform assertions for treatment level
    // Outpatient is not set in all the treatment level items
    if (result.get(OUTPATIENT.getValue()) != null) {
      assertEquals(outpatient, result.get(OUTPATIENT.getValue()).intValue());
    }
    assertEquals(normalWard, result.get(NORMAL_WARD.getValue()).intValue());
    assertEquals(icu, result.get(ICU.getValue()).intValue());
    assertEquals(icuVentilation, result.get(ICU_VENTILATION.getValue()).intValue());
    assertEquals(icuEcmo, result.get(ICU_ECMO.getValue()).intValue());
  }

  protected void assertCumulativeResults(
      DataItemContext context,
      String selectedDataItem,
      int positive,
      int borderlineSuspected,
      int negative) {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(context, selectedDataItem);

    Map<String, Number> result = (Map<String, Number>) dataItem.getData();

    // Perform assertions for treatment level
    assertEquals(positive, result.get(POSITIVE.getValue()).intValue());
    assertEquals(borderlineSuspected, result.get(BORDERLINE.getValue()).intValue());
    assertEquals(negative, result.get(NEGATIVE.getValue()).intValue());
  }

  protected void assertCumulativeGender(
      DataItemContext context, String selectedDataItem, int male, int female, int diverse) {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(context, selectedDataItem);

    Map<String, Number> result = (Map<String, Number>) dataItem.getData();

    // Perform assertions for treatment level
    assertEquals(male, result.get(MALE_SPECIFICATION.getValue()).intValue());
    assertEquals(female, result.get(FEMALE_SPECIFICATION.getValue()).intValue());
    assertEquals(diverse, result.get(DIVERSE_SPECIFICATION.getValue()).intValue());
  }

  protected void assertListEqual(
      DataItemContext context, String selectedDataItem, Collection<?> entries) {
    // Your existing implementation

    // Assuming sampleData and positive are defined somewhere in your test class
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    DiseaseDataItem dataItem = getTreatmentDataItem(context, selectedDataItem);
    List<?> result = (List<?>) dataItem.getData();

    // Check if dataResult contains all entries
    assertTrue(result.containsAll(entries));
  }

  protected void assertListIsSortedAscending(DataItemContext context, String selectedDataItem) {
    DiseaseDataItem dataItem = getTreatmentDataItem(context, selectedDataItem);
    List<?> result = (List<?>) dataItem.getData();

    // Check if the list is sorted in ascending order
    for (int i = 0; i < result.size() - 1; i++) {
      Comparable<Object> current = (Comparable<Object>) result.get(i);
      Comparable<Object> next = (Comparable<Object>) result.get(i + 1);
      assertTrue(current.compareTo(next) <= 0);
    }
  }

  protected void assertTimelineValueByDay(
      DataItemContext context, String selectedDataItem, Long timeStamp, long sum) {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(context, selectedDataItem);

    TimestampedListPair result = (TimestampedListPair) dataItem.getData();
    int indexByTimeStamp = result.getIndexByTimestamp(timeStamp);
    var resultValue = result.getPair(indexByTimeStamp).getValue();
    //    long resultValue = result.getValueByTimestamp(timeStamp);
    // Perform assertions for treatment level
    assertEquals(sum, resultValue);
  }

  protected void assertTimelineValueByDay(
      DataItemContext context,
      String selectedDataItem,
      Long timeStamp,
      long sum,
      TreatmentLevels treatmentLevel) {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(context, selectedDataItem);
    Map<String, List<Long>> result = (Map<String, List<Long>>) dataItem.getData();

    var resultByTreatmentlevel = result.get(treatmentLevel.getValue());
    int indexByTimeStamp = getIndexByTimestamp(timeStamp, result.get(DATE.getValue()));
    Long resultValue = resultByTreatmentlevel.get(indexByTimeStamp);
    // Perform assertions for treatment level
    assertEquals(sum, resultValue);
  }

  protected void assertListIsEmpty(DataItemContext context, String selectedDataItem) {
    assertListSize(context, selectedDataItem, 0);
  }

  protected void assertListSize(
      DataItemContext context, String selectedDataItem, Integer expectedSize) {
    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(context, selectedDataItem);
    List<?> dataItemType = (ArrayList<?>) dataItem.getData();
    assertEquals(expectedSize, dataItemType.size());
  }

  @Test
  abstract void testCurrentTreatmentLevel();

  @Test
  abstract void testCurrentMaxTreatmentLevel();

  @Test
  abstract void testCumulativeResults();
}
