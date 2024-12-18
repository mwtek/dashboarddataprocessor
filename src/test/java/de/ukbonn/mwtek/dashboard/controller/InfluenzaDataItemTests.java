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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.INFLUENZA;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_GENDER;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY_ICU_DEAD;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_MAXTREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_RESULTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_ZIPCODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_MAXTREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_TREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_MAXTREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_TESTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.TIMELINE_TEST_POSITIVE;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.ICU_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.TreatmentLevels.OUTPATIENT;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class InfluenzaDataItemTests extends DataItemTests {

  @Test
  @DisplayName("Testing infl.current.treatmentlevel")
  void testCurrentTreatmentLevel() {
    assertTreatmentLevel(INFLUENZA, CURRENT_TREATMENTLEVEL, null, 0, 0, 1, 0);
  }

  @Test
  @DisplayName("Testing infl.current.maxtreatmentlevel")
  void testCurrentMaxTreatmentLevel() {
    assertTreatmentLevel(INFLUENZA, CURRENT_MAXTREATMENTLEVEL, null, 0, 0, 1, 0);
  }

  @Test
  @DisplayName("Testing infl.cumulative.results")
  void testCumulativeResults() {
    assertCumulativeResults(INFLUENZA, CUMULATIVE_RESULTS, 2, 0, 1);
  }

  @Test
  @DisplayName("Testing infl.cumulative.zipcode")
  void testCumulativeZipCodes() {
    assertListEqual(INFLUENZA, CUMULATIVE_ZIPCODE, List.of("12345", "72453", "77777"));
    assertListIsSortedAscending(INFLUENZA, CUMULATIVE_ZIPCODE);
  }

  @Test
  @DisplayName("Testing infl.current.age.maxtreatmentlevel.icu_with_ventilation")
  void testCurrentAgeMaxTreatmentLevelVentilation() {
    assertListEqual(INFLUENZA, CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION, List.of(50L));
  }

  @Test
  @DisplayName("Testing infl.cumulative.gender")
  void testCumulativeGender() {
    assertCumulativeGender(INFLUENZA, CUMULATIVE_GENDER, 3, 1, 0);
  }

  // 1712448000
  @Test
  @DisplayName("Testing infl.timeline.tests")
  void testTimelineTests() {
    assertTimelineValueByDay(INFLUENZA, TIMELINE_TESTS, 1704672000L, 1);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_TESTS, 1704758400L, 1);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_TESTS, 1704844800L, 0);
  }

  @Test
  @DisplayName("Testing infl.timeline.test.positive")
  void testTimelineTestPositive() {
    assertTimelineValueByDay(INFLUENZA, TIMELINE_TEST_POSITIVE, 1704672000L, 0);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_TEST_POSITIVE, 1704758400L, 1);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_TEST_POSITIVE, 1704844800L, 0);
  }

  @Test
  @DisplayName("Testing infl.cumulative.lengthofstay.icu.dead")
  void testCumulativeLengthOfStayIcuDead() {
    assertListIsEmpty(INFLUENZA, CUMULATIVE_LENGTHOFSTAY_ICU_DEAD);
    assertListIsSortedAscending(INFLUENZA, CUMULATIVE_LENGTHOFSTAY_ICU_DEAD);
  }

  @Test
  @DisplayName("Testing infl.cumulative.maxtreatmentlevel")
  void testCumulativeMaxTreatmentlevel() {
    assertTreatmentLevel(INFLUENZA, CUMULATIVE_MAXTREATMENTLEVEL, 2, 0, 0, 1, 1);
  }

  @Test
  @DisplayName("Testing infl.timeline.maxtreatmentlevel")
  void testTimelineMaxTreatmentlevel() {
    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1708992000L, 0, OUTPATIENT);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1709164800L, 1, OUTPATIENT);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1709251200L, 0, OUTPATIENT);

    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1703980800L, 0, NORMAL_WARD);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1704067200L, 1, NORMAL_WARD);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1704153600L, 1, NORMAL_WARD);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1704240000L, 1, NORMAL_WARD);

    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1705190400L, 0, ICU);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1705276800L, 1, ICU);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1705363200L, 1, ICU);

    assertTimelineValueByDay(
        INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1712534400L, 0, ICU_VENTILATION);
    assertTimelineValueByDay(
        INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1712620800L, 1, ICU_VENTILATION);
    assertTimelineValueByDay(
        INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1712707200L, 1, ICU_VENTILATION);

    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1705622400L, 0, ICU_ECMO);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1705708800L, 1, ICU_ECMO);
    assertTimelineValueByDay(INFLUENZA, TIMELINE_MAXTREATMENTLEVEL, 1705795200L, 1, ICU_ECMO);
  }
}
