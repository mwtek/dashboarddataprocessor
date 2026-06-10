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

import static com.jayway.jsonpath.internal.path.PathCompiler.fail;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.BCT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_CONSENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_CONSENT;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent.BCT_MOD_1;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent.BCT_MOD_2;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent.BCT_MOD_3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BctDataItemTests extends DataItemTests {

  /** Load only KidsRadar sample data for this test class. */
  @BeforeAll
  void setupSampleData() throws IOException {
    // Calls the single-context helper provided by ResultFunctionalityTests
    sampleData = loadSampleData(BCT);
  }

  @Test
  @DisplayName("Testing bct.current.consent")
  void testCurrentConsent() {
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());
    verifyCurrentConsent(BCT, CURRENT_CONSENT, 4, 1);
  }

  @Test
  @DisplayName("Testing bct.timeline.consent")
  void testTimelineConsent() {
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    DiseaseDataItem dataItem = getTreatmentDataItem(BCT, TIMELINE_CONSENT);
    Map<String, List<Long>> result = (Map<String, List<Long>>) dataItem.getData();
    assertValueByTimestampEquals(1727740800L, result, BCT_MOD_1, 1);
    assertValueByTimestampEquals(1727740800L, result, BCT_MOD_2, 0);
    assertValueByTimestampEquals(1727740800L, result, BCT_MOD_3, 1);
    assertValueByTimestampEquals(1735603200L, result, BCT_MOD_1, 1);
    assertValueByTimestampEquals(1735603200L, result, BCT_MOD_2, 0);
    assertValueByTimestampEquals(1735603200L, result, BCT_MOD_3, 1);
    // 04.05.2025
    assertValueByTimestampEquals(1746316800L, result, BCT_MOD_1, 4);
    assertValueByTimestampEquals(1746316800L, result, BCT_MOD_2, 0);
    assertValueByTimestampEquals(1746316800L, result, BCT_MOD_3, 4);
    // 05.05.2025
    assertValueByTimestampEquals(1746403200L, result, BCT_MOD_1, 3);
    assertValueByTimestampEquals(1746403200L, result, BCT_MOD_2, 0);
    assertValueByTimestampEquals(1746403200L, result, BCT_MOD_3, 3);
  }

  protected void verifyCurrentConsent(
      DataItemContext context, String selectedDataItem, Integer sumBctMod1, Integer sumBctMod2) {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(context, selectedDataItem);
    assertNotNull(dataItem);
    assertNotNull(dataItem.getData());
    assertInstanceOf(StackedBarChartsItem.class, dataItem.getData());

    @SuppressWarnings("unchecked")
    StackedBarChartsItem<Integer> result = (StackedBarChartsItem<Integer>) dataItem.getData();
    assertEquals(sumBctMod1, result.getValueByBarChart(BCT_MOD_1).getFirst());
    assertEquals(sumBctMod2, result.getValueByBarChart(BCT_MOD_2).getFirst());
    // StackedBarChartsItem contains a debugData attribute which must not be part of the output!
    assertNoDebugDataInJson(result);
  }

  private void assertNoDebugDataInJson(StackedBarChartsItem<Integer> result) {
    assertNotNull(result);
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode jsonNode = objectMapper.valueToTree(result);
      assertFalse(jsonNode.has("debugData"), "JSON must not contain 'debugData'");
    } catch (IllegalArgumentException e) {
      fail("Serialization to JSON tree failed: " + e.getMessage());
    }
  }

  @Override
  void testCurrentTreatmentLevel() {}

  @Override
  void testCurrentMaxTreatmentLevel() {}

  @Override
  void testCumulativeResults() {}
}
