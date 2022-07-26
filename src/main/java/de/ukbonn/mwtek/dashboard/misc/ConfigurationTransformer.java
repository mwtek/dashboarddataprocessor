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
            .getOrDefault("procedure.ventilation", "40617009,57485005"));
      }
      case PROCEDURES_ECMO -> {
        return commaSeparatedStringIntoList(globalConfiguration.getInputCodes()
            .getOrDefault("procedure.ecmo", "182744004"));
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
