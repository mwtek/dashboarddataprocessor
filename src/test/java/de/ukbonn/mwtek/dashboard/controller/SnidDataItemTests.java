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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.SNID;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_RECRUITMENT_DIZ;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_RECRUITMENT_DIZ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.ParametersSetItem;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SnidDataItemTests extends DataItemTests {

  /** Load only SNID sample data for this test class. */
  @BeforeAll
  void setupSampleData() throws IOException {
    // Calls the single-context helper provided by ResultFunctionalityTests
    sampleData = loadSampleData(SNID);
  }

  @Test
  @DisplayName("Testing snid.current.recruitment.diz")
  void testCurrentConsent() {
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());
    verifyCurrentConsent(SNID, CURRENT_RECRUITMENT_DIZ, 2);
  }

  @Test
  @DisplayName("Testing snid.timeline.recruitment.diz")
  void testTimelineConsent() {
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // 05.05.2025 (kick off date)
    assertParametersTimelineValueByDay(SNID, TIMELINE_RECRUITMENT_DIZ, 1746403200L, 0);
    // 26.01.2026
    assertParametersTimelineValueByDay(SNID, TIMELINE_RECRUITMENT_DIZ, 1769385600L, 1);
    // 27.01.2026
    assertParametersTimelineValueByDay(SNID, TIMELINE_RECRUITMENT_DIZ, 1769472000L, 0);
    // 30.09.2026
    assertParametersTimelineValueByDay(SNID, TIMELINE_RECRUITMENT_DIZ, 1790726400L, 0);
  }

  protected void verifyCurrentConsent(
      DataItemContext context, String selectedDataItem, Integer sumCurrent) {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(context, selectedDataItem);
    assertNotNull(dataItem);
    assertNotNull(dataItem.getData());
    assertInstanceOf(ParametersSetItem.class, dataItem.getData());

    @SuppressWarnings("unchecked")
    ParametersSetItem<Integer> result = (ParametersSetItem<Integer>) dataItem.getData();
    assertEquals(sumCurrent, result.getValues().getFirst());
  }

  @Override
  void testCurrentTreatmentLevel() {}

  @Override
  void testCurrentMaxTreatmentLevel() {}

  @Override
  void testCumulativeResults() {}
}
