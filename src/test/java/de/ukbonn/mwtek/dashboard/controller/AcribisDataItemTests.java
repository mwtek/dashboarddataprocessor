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

import static de.ukbonn.mwtek.dashboardlogic.enums.DashboardLogicFixedValues.DATE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.ACRIBIS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_RECRUITMENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_RECRUITMENT;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentRecruitment.ACR_RECRUITMENT_CONSENT;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentRecruitment.ACR_RECRUITMENT_FOLLOWUP;
import static de.ukbonn.mwtek.dashboardlogic.tools.TimelineTools.getIndexByTimestamp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.models.DiseaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.models.StackedBarChartsItem;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
public class AcribisDataItemTests extends DataItemTests {

  @Test
  @DisplayName("Testing acr.current.recruitment")
  void testCurrentRecruitment() {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());
    verifyCurrentRecruitment(ACRIBIS, CURRENT_RECRUITMENT, 2, 0);
  }

  @Test
  @DisplayName("Testing acr.timeline.recruitment")
  void testTimelineRecruitment() {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    assertTimelineValueByDay(ACRIBIS, TIMELINE_RECRUITMENT, 1727740800L, 0, 0);
    assertTimelineValueByDay(ACRIBIS, TIMELINE_RECRUITMENT, 1737849600L, 1, 0);
    assertTimelineValueByDay(ACRIBIS, TIMELINE_RECRUITMENT, 1737936000L, 0, 0);
    assertTimelineValueByDay(ACRIBIS, TIMELINE_RECRUITMENT, 1738022400L, 1, 0);
  }

  protected void verifyCurrentRecruitment(
      DataItemContext context,
      String selectedDataItem,
      Integer sumRecruitmentConsent,
      Integer sumRecruitmentFollowUp) {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(context, selectedDataItem);
    StackedBarChartsItem result = (StackedBarChartsItem) dataItem.getData();
    assertEquals(sumRecruitmentConsent, result.getValueByBarChart(ACR_RECRUITMENT_CONSENT).get(0));
    assertEquals(
        sumRecruitmentFollowUp, result.getValueByBarChart(ACR_RECRUITMENT_FOLLOWUP).get(0));
  }

  protected void assertTimelineValueByDay(
      DataItemContext context,
      String selectedDataItem,
      Long timeStamp,
      long sumRecruitment,
      long sumFollowUp) {
    // Test setup
    assertNotNull(sampleData);
    assertFalse(sampleData.isEmpty());

    // Get the data item for treatment level
    DiseaseDataItem dataItem = getTreatmentDataItem(context, selectedDataItem);

    Map<String, List<? extends Number>> result =
        (Map<String, List<? extends Number>>) dataItem.getData();
    int indexByTimeStamp = getIndexByTimestamp(timeStamp, result.get(DATE.getValue()));
    var recruitmentValue = result.get(ACR_RECRUITMENT_CONSENT).get(indexByTimeStamp);
    var followUpValue = result.get(ACR_RECRUITMENT_FOLLOWUP).get(indexByTimeStamp);
    assertEquals(sumRecruitment, recruitmentValue);
    assertEquals(sumFollowUp, followUpValue);

    //    long resultValue = result.getValueByTimestamp(timeStamp);
    // Perform assertions for treatment level
  }

  @Override
  void testCurrentTreatmentLevel() {}

  @Override
  void testCurrentMaxTreatmentLevel() {}

  @Override
  void testCumulativeResults() {}
}
