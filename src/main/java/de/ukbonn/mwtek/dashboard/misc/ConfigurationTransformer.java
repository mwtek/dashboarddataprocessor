/*
 *
 *  Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 *  modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 *  PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 *  OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 *  IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 *  YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 *  OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 *  COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 *  BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 *  OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 *  PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 *
 */

package de.ukbonn.mwtek.dashboard.misc;

import static de.ukbonn.mwtek.dashboard.misc.ListHelper.commaSeparatedStringIntoList;

import de.ukbonn.mwtek.dashboard.configuration.GlobalConfiguration;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import java.util.List;

/**
 * Class that reads individually configurable terminology codes from YAML and converts them into
 * required formats and data types for data queries and data processing.
 */
public class ConfigurationTransformer {

  public static List<String> extractInputCode(GlobalConfiguration globalConfiguration,
      CONFIGURATION_CONTEXT context) {
    switch (context) {
      case OBSERVATIONS_PCR -> {
        return commaSeparatedStringIntoList(globalConfiguration.getInputCodes()
            .getOrDefault("observation.pcr", "94640-0,94306-8,96763-8"));
      }
      case OBSERVATIONS_VARIANTS -> {
        return commaSeparatedStringIntoList(globalConfiguration.getInputCodes()
            .getOrDefault("observation.variants", "96741-4,96895-8"));
      }
      case PROCEDURES_VENTILATION -> {
        return commaSeparatedStringIntoList(globalConfiguration.getInputCodes()
            .getOrDefault("procedure.ventilation",
                "26763009,243147009,26763009,243141005,11140008,428311008,243160003,182687005,286812008,33050008,45851008,55089006,8948006,45851008,229306004,243180002,243161004,243164007,182687005,243151006,59427005,286812008,47545007,243142003,37113006,243157005,243170001,371908008,243163001,243162006,243167000,243166009,243169002,243168005,243150007,243156001,243148004,243149007,243154003,243155002,243153009,243152004,286813003,408852001,408853006,286813003,448442005,405609003,66852000,4764004,243143008,448134000,229308003,34281000175105,34291000175108,243144002,229312009,425447009,243140006,229313004,425696007,243184006,74596007,243181003,243183000,243182005,52729008,76777009,38282001,773454006,281508008,276737004,276732005,243159008,243158000,243172009,243171002,82433009,447837008,870392006,243146000,429253002,371907003,398077001,182714002,243136002,315041000,426990007,304577004,870533002,71786000"));
      }
      case PROCEDURES_ECMO -> {
        return commaSeparatedStringIntoList(globalConfiguration.getInputCodes()
            .getOrDefault("procedure.ecmo",
                "265764009,341939001,127788007,233572003,302497006,233586004,233581009,182750009,714749008,233573008,233578004,233575001,233588003,233583007,233594006,72541002,233574002,786452006,786451004,786453001,233579007,233580005,708932005,427053002,57274006,233577009,233576000,11932001,698074000,233589006,233590002,708933000,233587008,708930002,233584001,233585000,715743002,233582002,708934006,14130001000004100,182749009,438839005,83794005,77257005,448955002,20720000,233564000,233565004,225067007,19647005"));
      }
      case CONDITIONS -> {
        return commaSeparatedStringIntoList(globalConfiguration.getInputCodes()
            .getOrDefault("condition", "U07.1,U07.2"));
      }
      default -> {
        return null;
      }
    }
  }

  /**
   * Some parts of the local configuration require individual handling in the reading and evaluation
   * process.
   */
  public enum CONFIGURATION_CONTEXT {
    OBSERVATIONS_PCR, OBSERVATIONS_VARIANTS, PROCEDURES_VENTILATION, PROCEDURES_ECMO, CONDITIONS
  }

  public static InputCodeSettings extractInputCodeSettings(
      AbstractDataRetrievalService dataRetrievalService) {
    return new InputCodeSettings(
        dataRetrievalService.getLabPcrCodes(),
        dataRetrievalService.getLabVariantCodes(), dataRetrievalService.getIcdCodes(),
        dataRetrievalService.getProcedureVentilationCodes(),
        dataRetrievalService.getProcedureEcmoCodes());
  }
}
