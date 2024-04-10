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

package de.ukbonn.mwtek.dashboard.examples;

import com.google.common.collect.ImmutableList;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import java.util.List;
import lombok.Getter;

public class InputCodeSettingsExampleData {

  @Getter
  private static final List<String> observationPcrLoincCodes = ImmutableList.of("94306-8",
      "96763-8",
      "94640-0");

  @Getter
  private static final List<String> observationVariantLoincCodes = ImmutableList.of("96741-4",
      "96895-8");

  @Getter
  private static final List<String> conditionIcdCodes = ImmutableList.of("U07.1", "U07.2");

  @Getter
  private static final List<String> procedureVentCodes = ImmutableList.of("26763009", "243147009",
      "26763009");

  @Getter
  private static final List<String> procedureEcmoCodes = ImmutableList.of("265764009", "341939001",
      "127788007");

  public static InputCodeSettings getExampleData() {
    return new InputCodeSettings(observationPcrLoincCodes, observationVariantLoincCodes,
        conditionIcdCodes, procedureVentCodes, procedureEcmoCodes, null, null);
  }

}
