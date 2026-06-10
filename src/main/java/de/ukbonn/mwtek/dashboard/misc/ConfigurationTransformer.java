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

package de.ukbonn.mwtek.dashboard.misc;

import static de.ukbonn.mwtek.dashboard.misc.AcuwaveQuerySuffixBuilder.DELIMITER;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.KJP;
import static de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext.PED;
import static de.ukbonn.mwtek.utilities.enums.TerminologySystems.SNOMED;
import static de.ukbonn.mwtek.utilities.generic.collections.ListTools.commaSeparatedStringIntoList;

import de.ukbonn.mwtek.dashboard.configuration.CustomGlobalConfiguration;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.enums.KidsRadarDataItemContext;
import de.ukbonn.mwtek.dashboardlogic.settings.InputCodeSettings;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Class that reads individually configurable terminology codes from YAML and converts them into
 * required formats and data types for data queries and data processing.
 */
@Slf4j
public class ConfigurationTransformer {

  public static final String DEFAULT_ICD_CODES_INFLUENZA = "J10.0,J10.1,J10.8,J09";
  public static final String DEFAULT_PCR_LOINC_INFLUENZA =
      "34487-9,60416-5,49521-8,49531-7," + "61365-3,48509-4,29909-9,40982-1";
  public static final String DEFAULT_ICD_CODES_COVID = "U07.1";
  public static final String DEFAULT_PCR_LOINC_COVID = "94640-0,94306-8,96763-8";
  public static final String DEFAULT_PCR_LOINC_COVID_VARIANTS = "96741-4,96895-8";
  public static final String DEFAULT_SNOMED_PROCEDURE_VENTILATION =
      "26763009,243147009,26763009,243141005,11140008,428311008,243160003,182687005,"
          + "286812008,33050008,45851008,55089006,8948006,45851008,229306004,243180002,"
          + "243161004,243164007,182687005,243151006,59427005,286812008,47545007,"
          + "243142003,37113006,243157005,243170001,371908008,243163001,243162006,"
          + "243167000,243166009,243169002,243168005,243150007,243156001,243148004,"
          + "243149007,243154003,243155002,243153009,243152004,286813003,408852001,"
          + "408853006,286813003,448442005,405609003,66852000,4764004,243143008,"
          + "448134000,229308003,34281000175105,34291000175108,243144002,229312009,"
          + "425447009,243140006,229313004,425696007,243184006,74596007,243181003,"
          + "243183000,243182005,52729008,76777009,38282001,773454006,281508008,"
          + "276737004,276732005,243159008,243158000,243172009,243171002,82433009,"
          + "447837008,870392006,243146000,429253002,371907003,398077001,182714002,"
          + "243136002,315041000,426990007,304577004,71786000";
  public static final String DEFAULT_SNOMED_PROCEDURE_ECMO =
      "265764009,341939001,127788007,233572003,302497006,233586004,233581009,182750009,"
          + "714749008,233573008,233578004,233575001,233588003,233583007,233594006,"
          + "72541002,233574002,786452006,786451004,786453001,233579007,233580005,"
          + "708932005,427053002,57274006,233577009,233576000,11932001,698074000,"
          + "233589006,233590002,708933000,233587008,708930002,233584001,233585000,"
          + "715743002,233582002,708934006,14130001000004100,182749009,438839005,"
          + "83794005,77257005,448955002,20720000,233564000,233565004,225067007,"
          + "19647005";
  public static final String CPAP_SNOMED_CODE = "47545007";
  public static final String HIGH_FLOW_SNOMED_CODE = "870533002";

  public static final List<String> DEFAULT_SNOMED_ICU_PROCEDURES_KIRA_PED;
  public static final String KIDS_RADAR_STR = "kids-radar";

  static {
    List<String> procedures = new ArrayList<>();
    procedures.addAll(commaSeparatedStringIntoList(DEFAULT_SNOMED_PROCEDURE_VENTILATION));
    procedures.addAll(commaSeparatedStringIntoList(DEFAULT_SNOMED_PROCEDURE_ECMO));
    procedures.add(CPAP_SNOMED_CODE);
    procedures.add(HIGH_FLOW_SNOMED_CODE);
    DEFAULT_SNOMED_ICU_PROCEDURES_KIRA_PED = Collections.unmodifiableList(procedures);
  }

  public static final String DEFAULT_UKB_MODEL_LOINC_CODES_OBSERVATIONS =
      "2160-0, 32693-4, " + "3091-6, 9187-6";

  public static final String DEFAULT_KIDS_RADAR_RSV_ACUTE_BRONCHIOLITIS = "J21.0";
  public static final String DEFAULT_KIDS_RADAR_RSV_ACUTE_CAUSED_DISEASE = "B97.4";
  public static final String DEFAULT_KIDS_RADAR_RSV_ACUTE_BRONCHITIS = "J20.5";
  public static final String DEFAULT_KIDS_RADAR_RSV_PNEUMONIA = "J12.1";

  public static final String DEFAULT_KIDS_RADAR_STRESS_RELATED_DISORDERS =
      "F43.0,F43.1,F43.2,F43.8,F43.9,F44.0,F44.1,F44.2,F44.3,F44.4,F44.5,F44.6,F44.6+,F44.7,F44.8,F44.80,F44.81,F44.82,F44.88,F44.88,F44.9,F94.1,F94.2";

  public static final String DEFAULT_KIDS_RADAR_OTHER_PSYCHOLOGICAL_DISORDERS =
      "F00.0,F00.1,F00.2,F00.9,F01.0,F01.1,F01.2,F01.3,F01.8,F01.9,F02.0,F02.1,F02.2,F02.3,F02.4,F02.8,F03,F04,F05.0,F05.1,F05.8,F05.9,F06.0,F06.1,F06.2,F06.3,F06.4,F06.5,F06.6,F06.7,F06.8,F06.9,F07.0,F07.1,F07.2,F07.8,F07.9,F09,F45.0,F45.1,F45.2,F45.3,F45.30,F45.31,F45.32,F45.33,F45.34,F45.37,F45.38,F45.39,F45.4,F45.40,F45.41,F45.8,F45.9,F48.0,F48.1,F48.8,F48.9,F51.0,F51.1,F51.2,F51.3,F51.4,F51.5,F51.8,F51.9,F52.0,F52.1,F52.2,F52.3,F52.4,F52.5,F52.6,F52.7,F52.8,F52.9,F53.0,F53.1,F53.8,F53.9,F54,F55.0,F55.1,F55.2,F55.3,F55.4,F55.5,F55.6,F55.8,F55.9,F59,F60.0,F60.1,F60.2,F60.3,F60.30,F60.31,F60.4,F60.5,F60.6,F60.7,F60.8,F60.9,F61,F62.0,F62.1,F62.8,F62.80,F62.88,F62.9,F63.0,F63.1,F63.2,F63.3,F63.8,F63.9,F64.0,F64.1,F64.2,F64.8,F64.9,F65.0,F65.1,F65.2,F65.3,F65.4,F65.5,F65.6,F65.8,F65.9,F66.0,F66.1,F66.2,F66.8,F66.9,F68.0,F68.1,F68.8,F69,F95.0,F95.1,F95.2,F95.8,F95.9,F98.0,F98.00,F98.01,F98.02,F98.08,F98.1,F98.2,F98.3,F98.4,F98.40,F98.41,F98.49,F98.8,F98.80,F98.88,F98.9,F99";

  public static final String DEFAULT_KIDS_RADAR_PERTUSSIS_LOINC =
      "43913-3,23826-1,92128-8,82179-3,42588-4,82177-7,80601-8";
  public static final String DEFAULT_KIDS_RADAR_RSV_LOINC = "40988-8,92131-2";

  public static final Map<String, List<String>> DEFAULT_KIDS_RADAR_MAP;

  public static final String STRESS_RELATED_DISORDERS = "stress-related-disorders";
  public static final String OTHER_PSYCHOLOGICAL_DISORDERS = "other-psychological-disorders";

  // Initialize a map with default values by scope
  static {
    DEFAULT_KIDS_RADAR_MAP = new HashMap<>();
    DEFAULT_KIDS_RADAR_MAP.put(
        STRESS_RELATED_DISORDERS,
        commaSeparatedStringIntoList(DEFAULT_KIDS_RADAR_STRESS_RELATED_DISORDERS));
    DEFAULT_KIDS_RADAR_MAP.put(
        OTHER_PSYCHOLOGICAL_DISORDERS,
        commaSeparatedStringIntoList(DEFAULT_KIDS_RADAR_OTHER_PSYCHOLOGICAL_DISORDERS));
  }

  public static final Map<String, List<String>> DEFAULT_KIDS_RADAR_RSV_DIAGNOSIS_MAP;

  public static final Map<String, List<String>> DEFAULT_KIDS_RADAR_PED_LOINC_MAP;

  public static final String DEFAULT_KIDS_RADAR_KJP_OPS_CODE_PREFIX = "9-67";

  public static final String PERTUSSIS_1 = "A37.0";
  public static final String PERTUSSIS_2 = "A37.1";
  public static final String PERTUSSIS_3 = "A37.8";
  public static final String PERTUSSIS_4 = "A37.9";
  public static final List<String> DEFAULT_KIDS_RADAR_PED_DIAGNOSIS =
      List.of(
          DEFAULT_KIDS_RADAR_RSV_ACUTE_BRONCHIOLITIS,
          DEFAULT_KIDS_RADAR_RSV_ACUTE_CAUSED_DISEASE,
          DEFAULT_KIDS_RADAR_RSV_ACUTE_BRONCHITIS,
          DEFAULT_KIDS_RADAR_RSV_PNEUMONIA,
          "J10.0",
          "J10.1",
          "J10.8",
          "U69.24",
          "U07.1",
          PERTUSSIS_1,
          PERTUSSIS_2,
          PERTUSSIS_3,
          PERTUSSIS_4);

  public static final List<String> DEFAULT_KIDS_RADAR_PED_PERTUSSIS =
      List.of(PERTUSSIS_1, PERTUSSIS_2, PERTUSSIS_3, PERTUSSIS_4);

  public static final String RSV_DIAGNOSES_ALL = "rsv_diagnoses_all";
  public static final String RSV_ACUTE_BRONCHIOLITIS = "rsv_acute_bronchiolitis";
  public static final String RSV_CAUSED_DISEASE = "rsv_caused_disease";
  public static final String RSV_ACUTE_BRONCHITIS = "rsv_acute_bronchitis";
  public static final String RSV_PNEUMONIA = "rsv_pneumonia";

  public static final String PERTUSSIS = "pertussis";

  // Initialize a map with default values by scope
  static {
    DEFAULT_KIDS_RADAR_RSV_DIAGNOSIS_MAP = new HashMap<>();
    DEFAULT_KIDS_RADAR_RSV_DIAGNOSIS_MAP.put(
        RSV_ACUTE_BRONCHIOLITIS,
        commaSeparatedStringIntoList(DEFAULT_KIDS_RADAR_RSV_ACUTE_BRONCHIOLITIS));
    DEFAULT_KIDS_RADAR_RSV_DIAGNOSIS_MAP.put(
        RSV_CAUSED_DISEASE,
        commaSeparatedStringIntoList(DEFAULT_KIDS_RADAR_RSV_ACUTE_CAUSED_DISEASE));
    DEFAULT_KIDS_RADAR_RSV_DIAGNOSIS_MAP.put(
        RSV_ACUTE_BRONCHITIS,
        commaSeparatedStringIntoList(DEFAULT_KIDS_RADAR_RSV_ACUTE_BRONCHITIS));
    DEFAULT_KIDS_RADAR_RSV_DIAGNOSIS_MAP.put(
        RSV_PNEUMONIA, commaSeparatedStringIntoList(DEFAULT_KIDS_RADAR_RSV_PNEUMONIA));
    DEFAULT_KIDS_RADAR_RSV_DIAGNOSIS_MAP.put(PERTUSSIS, DEFAULT_KIDS_RADAR_PED_PERTUSSIS);

    DEFAULT_KIDS_RADAR_PED_LOINC_MAP = new HashMap<>();
    DEFAULT_KIDS_RADAR_PED_LOINC_MAP.put(
        PERTUSSIS, commaSeparatedStringIntoList(DEFAULT_KIDS_RADAR_PERTUSSIS_LOINC));
    DEFAULT_KIDS_RADAR_PED_LOINC_MAP.put(
        "rsv", commaSeparatedStringIntoList(DEFAULT_KIDS_RADAR_RSV_LOINC));
  }

  private static final Map<String, Object> EMPTY_MAP = Collections.emptyMap();

  public static List<String> extractInputCodes(
      CustomGlobalConfiguration customGlobalConfiguration, ConfigurationContext context) {
    final Map<String, Object> inputCodes = customGlobalConfiguration.getInputCodes();

    Map<String, Object> influenzaData =
        (Map<String, Object>) inputCodes.getOrDefault("influenza", EMPTY_MAP);
    Map<String, Object> covidObservationData =
        (Map<String, Object>) inputCodes.getOrDefault("observation", EMPTY_MAP);
    Map<String, Object> covidProcedureData =
        (Map<String, Object>) inputCodes.getOrDefault("procedure", EMPTY_MAP);
    Map<String, Object> predictionModelData =
        (Map<String, Object>) inputCodes.getOrDefault("prediction-models", EMPTY_MAP);
    Map<String, ?> kidsRadarBase = (Map<String, ?>) inputCodes.get(KIDS_RADAR_STR);
    Map<String, Object> kidsRadarPed = (Map<String, Object>) kidsRadarBase.get("ped");

    switch (context) {
      case COVID_OBSERVATIONS_PCR -> {
        return getCommaSeparatedList(covidObservationData, "pcr", DEFAULT_PCR_LOINC_COVID);
      }
      case COVID_OBSERVATIONS_VARIANTS -> {
        return getCommaSeparatedList(
            covidObservationData, "variants", DEFAULT_PCR_LOINC_COVID_VARIANTS);
      }
      case COVID_PROCEDURES_VENTILATION -> {
        return getCommaSeparatedList(
            covidProcedureData, "ventilation", DEFAULT_SNOMED_PROCEDURE_VENTILATION);
      }
      case COVID_PROCEDURES_ECMO -> {
        return getCommaSeparatedList(covidProcedureData, "ecmo", DEFAULT_SNOMED_PROCEDURE_ECMO);
      }
      case COVID_CONDITIONS -> {
        return commaSeparatedStringIntoList(
            (String) inputCodes.getOrDefault("condition", DEFAULT_ICD_CODES_COVID));
      }
      case PREDICTION_MODEL_UKB_OBS_CODES -> {
        Map<String, Object> predictionModelUkbData =
            (Map<String, Object>)
                predictionModelData.getOrDefault(
                    "ukb-renal-replacement-procedures", new LinkedHashMap<>());
        return getCommaSeparatedList(
            predictionModelUkbData, "observations", DEFAULT_UKB_MODEL_LOINC_CODES_OBSERVATIONS);
      }
      case INFLUENZA_CONDITIONS -> {
        return getCommaSeparatedList(influenzaData, "conditions", DEFAULT_ICD_CODES_INFLUENZA);
      }
      case INFLUENZA_OBSERVATIONS_PCR -> {
        return getCommaSeparatedList(influenzaData, "observations", DEFAULT_PCR_LOINC_INFLUENZA);
      }
      case KIDS_RADAR_HIGH_FLOW -> {
        return getCommaSeparatedList(kidsRadarPed, "ventilation-highflow", HIGH_FLOW_SNOMED_CODE);
      }
      case KIDS_RADAR_CPAP -> {
        return getCommaSeparatedList(kidsRadarPed, "ventilation-cpap", CPAP_SNOMED_CODE);
      }
      case KIDS_RADAR_OPS_BASE -> {
        Object prefix = kidsRadarBase.get("kjp-ops-prefix");
        if (prefix instanceof String opsCode && !opsCode.isEmpty()) {
          return List.of(opsCode);
        } else {
          return List.of(DEFAULT_KIDS_RADAR_KJP_OPS_CODE_PREFIX);
        }
      }
      default -> throw new IllegalArgumentException("Unknown context type: " + context);
    }
  }

  private static List<String> getCommaSeparatedList(
      Map<String, Object> data, String key, String defaultValue) {

    String value = Optional.ofNullable(data.get(key)).map(String::valueOf).orElse(defaultValue);
    return commaSeparatedStringIntoList(value);
  }

  /**
   * Some parts of the local configuration require individual handling in the reading and evaluation
   * process.
   */
  public enum ConfigurationContext {
    COVID_OBSERVATIONS_PCR,
    COVID_OBSERVATIONS_VARIANTS,
    COVID_PROCEDURES_VENTILATION,
    COVID_PROCEDURES_ECMO,
    COVID_CONDITIONS,
    PREDICTION_MODEL_UKB_OBS_CODES,
    INFLUENZA_CONDITIONS,
    INFLUENZA_OBSERVATIONS_PCR,
    KIDS_RADAR_CONDITIONS,
    KIDS_RADAR_HIGH_FLOW,
    KIDS_RADAR_CPAP,
    KIDS_RADAR_PERTUSSIS,
    KIDS_RADAR_OPS_BASE
  }

  public static InputCodeSettings extractInputCodeSettings(
      AbstractDataRetrievalService dataRetrievalService) {
    return new InputCodeSettings(
        dataRetrievalService.getCovidLabPcrCodes(),
        dataRetrievalService.getCovidLabVariantCodes(),
        dataRetrievalService.getCovidIcdCodes(),
        dataRetrievalService.getProcedureHighFlowCodes(),
        dataRetrievalService.getProcedureCpapCodes(),
        dataRetrievalService.getProcedureVentilationCodes(),
        dataRetrievalService.getProcedureEcmoCodes(),
        dataRetrievalService.getInfluenzaLabPcrCodes(),
        dataRetrievalService.getInfluenzaIcdCodes(),
        extractKidsRadarDiagnosisConditions(
            dataRetrievalService.getCustomGlobalConfiguration(), KJP),
        extractKidsRadarDiagnosisConditions(
            dataRetrievalService.getCustomGlobalConfiguration(), PED),
        extractKidsRadarLoincCodes(dataRetrievalService.getCustomGlobalConfiguration(), PED),
        dataRetrievalService.getKjpOpsCodePrefix());
  }

  public static QualitativeLabCodesSettings extractQualitativeLabCodesSettings(
      AbstractDataRetrievalService dataRetrievalService) {
    return new QualitativeLabCodesSettings(
        extractLabCodes(
            dataRetrievalService.getCustomGlobalConfiguration().getQualitativeLabCodes(),
            "positive"),
        extractLabCodes(
            dataRetrievalService.getCustomGlobalConfiguration().getQualitativeLabCodes(),
            "borderline"),
        extractLabCodes(
            dataRetrievalService.getCustomGlobalConfiguration().getQualitativeLabCodes(),
            "negative"),
        // The code system is snomed by default
        dataRetrievalService
            .getCustomGlobalConfiguration()
            .getQualitativeLabCodes()
            .getOrDefault("systems", List.of(SNOMED)));
  }

  private static List<String> extractLabCodes(
      Map<String, List<String>> qualitativeLabCodes, String key) {
    return qualitativeLabCodes.get(key);
  }

  /**
   * Extracts codes (e.g., ICD or LOINC) dynamically from the global configuration for the given
   * Kids Radar context.
   *
   * <p>If the context object contains a nested map under {@code codeKey}, that nested map will be
   * used (e.g., for PED*). Otherwise, the top-level context map will be used (e.g., for KJP). In
   * addition, top-level overrides from {@code inputCodes} with the same key are still applied.
   *
   * @param customGlobalConfiguration the global configuration containing input codes
   * @param kidsRadarDataItemContext the target context (KJP or PED)
   * @param codeKey the code key inside the context map (e.g., "icd" or "loinc")
   * @return a map assigning diagnosis group keys to lists of codes
   * @throws NullPointerException if any argument is null
   */
  public static Map<String, List<String>> extractKidsRadarCodes(
      CustomGlobalConfiguration customGlobalConfiguration,
      KidsRadarDataItemContext kidsRadarDataItemContext,
      String codeKey) {

    Objects.requireNonNull(customGlobalConfiguration, "customGlobalConfiguration");
    Objects.requireNonNull(kidsRadarDataItemContext, "kidsRadarDataItemContext");
    Objects.requireNonNull(codeKey, "codeKey");

    Map<String, List<String>> result = new LinkedHashMap<>();
    final Map<String, Object> inputCodes = customGlobalConfiguration.getInputCodes();
    if (inputCodes == null) {
      return result;
    }

    // Map enum -> kids-radar context key
    final String kidsRadarContext =
        (kidsRadarDataItemContext == KidsRadarDataItemContext.KJP) ? "kjp" : "ped";

    Object kidsRadarBaseObj = inputCodes.get(KIDS_RADAR_STR);
    if (!(kidsRadarBaseObj instanceof Map<?, ?> kidsRadarBase)) {
      return result;
    }

    Object kidsRadarContextObj = kidsRadarBase.get(kidsRadarContext);
    if (!(kidsRadarContextObj instanceof Map<?, ?> kidsRadarContextMap)) {
      return result;
    }

    // If there is a nested map under codeKey (e.g., "icd" or "loinc"), use it;
    // otherwise, use the top-level map of the context.
    Object nestedCodesObj = kidsRadarContextMap.get(codeKey);
    final Map<?, ?> codesMap = (nestedCodesObj instanceof Map<?, ?> m) ? m : kidsRadarContextMap;

    for (Map.Entry<?, ?> entry : codesMap.entrySet()) {
      final String key = String.valueOf(entry.getKey());
      final String configuredValue = String.valueOf(entry.getValue());

      // Allow overrides from inputCodes (keeps the original behavior)
      final String effectiveValue = String.valueOf(inputCodes.getOrDefault(key, configuredValue));

      result.put(key, commaSeparatedStringIntoList(effectiveValue));
    }

    return result;
  }

  /**
   * Extracts ICD codes for kiradar. Internally delegates to {@link #extractKidsRadarCodes} with
   * {@code codeKey="icd"}.
   */
  public static Map<String, List<String>> extractKidsRadarDiagnosisConditions(
      CustomGlobalConfiguration customGlobalConfiguration,
      KidsRadarDataItemContext kidsRadarDataItemContext) {
    return extractKidsRadarCodes(customGlobalConfiguration, kidsRadarDataItemContext, "icd");
  }

  /**
   * Extracts LOINC codes for kiradar. Internally delegates to {@link #extractKidsRadarCodes} with
   * {@code codeKey="loinc"}.
   */
  public static Map<String, List<String>> extractKidsRadarLoincCodes(
      CustomGlobalConfiguration customGlobalConfiguration,
      KidsRadarDataItemContext kidsRadarDataItemContext) {
    return extractKidsRadarCodes(customGlobalConfiguration, kidsRadarDataItemContext, "loinc");
  }

  public static String getListAsString(Collection<?> idList) {
    return StringUtils.join(idList, DELIMITER);
  }
}
