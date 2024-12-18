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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.COVID;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_OUTPATIENT;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_GENDER;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_LENGTHOFSTAY_ICU_DEAD;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_MAXTREATMENTLEVEL;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_RESULTS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_ZIPCODE;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CURRENT_AGE_MAXTREATMENTLEVEL_NORMAL_WARD;
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

public class CovidDataItemTests extends DataItemTests {

  @Test
  @DisplayName("Testing current.treatmentlevel")
  void testCurrentTreatmentLevel() {
    assertTreatmentLevel(COVID, CURRENT_TREATMENTLEVEL, null, 0, 0, 0, 1);
  }

  @Test
  @DisplayName("Testing current.maxtreatmentlevel")
  void testCurrentMaxTreatmentLevel() {
    assertTreatmentLevel(COVID, CURRENT_MAXTREATMENTLEVEL, null, 0, 0, 0, 1);
  }

  @Test
  @DisplayName("Testing current.age.maxtreatmentlevel.icu_with_ecmo")
  void testCurrentAgeMaxTreatmentLevelEcmo() {
    assertListEqual(COVID, CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO, List.of(70L));
  }

  @Test
  @DisplayName("Testing cumulative.zipcode")
  void testCumulativeZipCodes() {
    assertListEqual(COVID, CUMULATIVE_ZIPCODE, List.of("12345", "33333", "77777"));
    assertListIsSortedAscending(COVID, CUMULATIVE_ZIPCODE);
  }

  @Test
  @DisplayName("Testing timeline.tests")
  void testTimelineTests() {
    assertTimelineValueByDay(COVID, TIMELINE_TESTS, 1661817600L, 0);
    assertTimelineValueByDay(COVID, TIMELINE_TESTS, 1661904000L, 1);
    assertTimelineValueByDay(COVID, TIMELINE_TESTS, 1661990400L, 1);
  }

  @Test
  @DisplayName("Testing timeline.test.positive")
  void testTimelineTestPositive() {
    assertTimelineValueByDay(COVID, TIMELINE_TEST_POSITIVE, 1661817600L, 0);
    assertTimelineValueByDay(COVID, TIMELINE_TEST_POSITIVE, 1661904000L, 0);
    assertTimelineValueByDay(COVID, TIMELINE_TEST_POSITIVE, 1661990400L, 1);
  }

  @Test
  @DisplayName("Testing cumulative.results")
  void testCumulativeResults() {
    assertCumulativeResults(COVID, CUMULATIVE_RESULTS, 3, 0, 1);
  }

  @Test
  @DisplayName("Testing cumulative.gender")
  void testCumulativeGender() {
    assertCumulativeGender(COVID, CUMULATIVE_GENDER, 3, 1, 0);
  }

  @Test
  @DisplayName("Testing cumulative.lengthofstay.icu.dead")
  void testCumulativeLengthOfStayIcuDead() {
    assertListEqual(COVID, CUMULATIVE_LENGTHOFSTAY_ICU_DEAD, List.of(223L));
    assertListIsSortedAscending(COVID, CUMULATIVE_LENGTHOFSTAY_ICU_DEAD);
  }

  @Test
  @DisplayName("Testing cumulative.maxtreatmentlevel")
  void testCumulativeMaxTreatmentlevel() {
    assertTreatmentLevel(COVID, CUMULATIVE_MAXTREATMENTLEVEL, 1, 0, 0, 1, 2);
  }

  @Test
  @DisplayName("Testing timeline.maxtreatmentlevel")
  void testTimelineMaxTreatmentlevel() {
    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1614384000L, 0, OUTPATIENT);
    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1614470400L, 1, OUTPATIENT);
    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1614556800L, 0, OUTPATIENT);

    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1672444800L, 0, NORMAL_WARD);
    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1672531200L, 0, NORMAL_WARD);
    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1672617600L, 0, NORMAL_WARD);

    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1659225600L, 0, ICU);
    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1659312000L, 1, ICU);
    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1659398400L, 2, ICU);

    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1659225600L, 0, ICU_VENTILATION);
    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1659312000L, 0, ICU_VENTILATION);
    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1659398400L, 1, ICU_VENTILATION);

    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1664496000L, 0, ICU_ECMO);
    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1664582400L, 1, ICU_ECMO);
    assertTimelineValueByDay(COVID, TIMELINE_MAXTREATMENTLEVEL, 1664668800L, 1, ICU_ECMO);
  }

  @Test
  @DisplayName("Testing current.age.maxtreatmentlevel")
  void testCurrentAgeMaxTreatmentlevel() {
    // No dedicated entry tests since the values increase in time
    assertListIsEmpty(COVID, CURRENT_AGE_MAXTREATMENTLEVEL_NORMAL_WARD);
    assertListIsEmpty(COVID, CURRENT_AGE_MAXTREATMENTLEVEL_ICU);
    assertListIsEmpty(COVID, CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION);
    assertListSize(COVID, CURRENT_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO, 1);
  }

  @Test
  @DisplayName("Testing cumulative.age.maxtreatmentlevel")
  void testCumulativeAgeMaxTreatmentlevel() {
    assertListEqual(COVID, CUMULATIVE_AGE_MAXTREATMENTLEVEL_OUTPATIENT, List.of(60));
    assertListEqual(COVID, CUMULATIVE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD, List.of());
    assertListEqual(COVID, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU, List.of());
    assertListEqual(COVID, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION, List.of(90));
    assertListEqual(COVID, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO, List.of(70, 70));
  }
}
