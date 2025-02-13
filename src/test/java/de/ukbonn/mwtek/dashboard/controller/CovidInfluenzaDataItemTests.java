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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItems.CUMULATIVE_INPATIENT_AGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests that are used for covid and influenza items */
public abstract class CovidInfluenzaDataItemTests extends DataItemTests {

  /** Each subclass provides its own context (COVID or INFLUENZA). */
  protected abstract DataItemContext getContext();

  @Test
  @DisplayName("Verify cumulative age sums across treatment levels")
  void testCumulativeAgeSums() {
    DataItemContext context = getContext();

    // Fetch treatment data
    List<Integer> inpatientAges = getDataAsIntegerList(context, CUMULATIVE_INPATIENT_AGE);
    List<Integer> normalWardAges =
        getDataAsIntegerList(context, CUMULATIVE_AGE_MAXTREATMENTLEVEL_NORMAL_WARD);
    List<Integer> icuAges = getDataAsIntegerList(context, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU);
    List<Integer> icuWithVentilationAges =
        getDataAsIntegerList(context, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_VENTILATION);
    List<Integer> icuWithEcmoAges =
        getDataAsIntegerList(context, CUMULATIVE_AGE_MAXTREATMENTLEVEL_ICU_WITH_ECMO);

    // Ensure no lists are null
    assertNotNull(inpatientAges, "Inpatient age list should not be null");
    assertNotNull(normalWardAges, "Normal ward age list should not be null");
    assertNotNull(icuAges, "ICU age list should not be null");
    assertNotNull(icuWithVentilationAges, "ICU with ventilation age list should not be null");
    assertNotNull(icuWithEcmoAges, "ICU with ECMO age list should not be null");

    // Validate list sizes
    int totalOtherListSize =
        normalWardAges.size()
            + icuAges.size()
            + icuWithVentilationAges.size()
            + icuWithEcmoAges.size();
    assertEquals(
        inpatientAges.size(),
        totalOtherListSize,
        "The size of inpatient list does not match combined size of other levels");

    // Validate age sums
    int sumInpatientAges = inpatientAges.stream().mapToInt(Integer::intValue).sum();
    int sumOtherAges =
        normalWardAges.stream().mapToInt(Integer::intValue).sum()
            + icuAges.stream().mapToInt(Integer::intValue).sum()
            + icuWithVentilationAges.stream().mapToInt(Integer::intValue).sum()
            + icuWithEcmoAges.stream().mapToInt(Integer::intValue).sum();

    assertEquals(
        sumInpatientAges,
        sumOtherAges,
        "The sum of inpatient ages does not match sum of all other treatment levels");
  }
}
