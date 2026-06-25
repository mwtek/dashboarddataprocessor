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

import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.getListAsString;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.ACRIBIS;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.COVID;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.INFLUENZA;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR;
import static de.ukbonn.mwtek.utilities.enums.ConsentFixedValues.CONSENT_CATEGORY_CODE;
import static de.ukbonn.mwtek.utilities.enums.ConsentFixedValues.CONSENT_CATEGORY_SYSTEM;
import static de.ukbonn.mwtek.utilities.enums.ConsentFixedValues.CONSENT_CATEGORY_SYSTEM_2026;

import de.ukbonn.mwtek.dashboard.configuration.FhirSearchConfiguration;
import de.ukbonn.mwtek.dashboard.interfaces.DataSourceType;
import de.ukbonn.mwtek.dashboard.interfaces.QuerySuffixBuilder;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.AcuwaveDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.FhirDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.utilities.enums.TerminologySystems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Building the templates of the individual REST requests to the Acuwaveles server. */
public class FhirServerQuerySuffixBuilder implements QuerySuffixBuilder {

  private static final String COUNT_EQUALS = "&_count=";
  private static final String DELIMITER = ",";
  public static final String SUMMARY_COUNT = "&_summary=count";
  public static final String SUMMARY_COUNT_PARAM = "_summary=count";
  public static final String DATE_GE = "&date=ge";
  public static final String RECORDED_DATE_GE = "&recorded-date=ge";
  public static final String CODE_PARAM = "code=";
  public static final String PRETTY_FALSE_PARAM = "&_pretty=false";
  // public static final String CLASS_IMP = "&class=IMP";
  public static final String PIPE = "|";
  public static final String DELIMITER_AND = "&";

  /** Ensures the date starts at 00:00:00 to include all events occurring on the specified day. */
  public static final String MIDNIGHT_TS = "T00:00:00";

  public static final String OBSERVATION_CALL = "Observation?";
  public static final String CONDITION_CALL = "Condition?";
  public static final String PATIENT_PARAM = "patient=";
  public static final String ENCOUNTER_PARAM = "encounter=";
  public static final String ENCOUNTER_INCLUDE_DIAGNOSIS = "&_include=Encounter:diagnosis";
  public static final String SUBJECT_PARAM = "subject=";
  public static final String URL_PARAM = "url=";
  public static final String QUESTIONNAIRE_PARAM = "questionnaire=";
  public static final String ENCOUNTER_CALL = "Encounter?";
  public static final String ID_START_PARAM = "_id=";
  public static final String LOCATION_CALL = "Location?";
  public static final String QUESTIONNAIRE_RESPONSE_CALL = "QuestionnaireResponse?";
  public static final String QUESTIONNAIRE_CALL = "Questionnaire?";
  public static final String PROCEDURE_CALL = "Procedure?";
  public static final String QUESTIONNAIRE_RESOURCE_URL =
      "https://www.medizininformatik-initiative.de/fhir/ext/modul-kardio/Questionnaire/MII-QN-Kardio-Acribis-Study-FollowUp";
  public static final String REV_INCLUDE_ENCOUNTER_DIAGNOSIS = "&_revinclude=Encounter:diagnosis";
  public static final String PATIENT_CALL = "Patient?";
  public static final String COUNT_EQUALS_PARAM = "_count=";

  public String getObservations(
      AbstractDataRetrievalService dataRetrievalService,
      Integer month,
      boolean summary,
      DataItemContext dataItemContext,
      boolean httpMethodGet) {
    // Start building only the query parameters
    StringBuilder suffixBuilder = new StringBuilder();

    // Build query parameters based on the dataItemContext
    switch (dataItemContext) {
      case COVID -> {
        // Join COVID PCR and variant codes
        String labPcrCodes = String.join(DELIMITER, dataRetrievalService.getCovidLabPcrCodes());
        String labVariantCodes =
            String.join(DELIMITER, dataRetrievalService.getCovidLabVariantCodes());

        // Append the codes as a query parameter
        suffixBuilder.append(CODE_PARAM).append(labPcrCodes);
        if (!labVariantCodes.isBlank()) {
          suffixBuilder.append(DELIMITER).append(labVariantCodes);
        }

        // Append the starting date filter for COVID observations
        if (dataRetrievalService.getFilterResourcesByDate()) {
          suffixBuilder.append(DATE_GE).append(getStartingDate(COVID));
        }
      }
      case INFLUENZA -> {
        // Join influenza PCR codes
        String labPcrCodes = String.join(DELIMITER, dataRetrievalService.getInfluenzaLabPcrCodes());

        // Append the codes as a query parameter
        suffixBuilder.append(CODE_PARAM).append(labPcrCodes);

        // Append the starting date filter for influenza observations
        if (dataRetrievalService.getFilterResourcesByDate()) {
          suffixBuilder.append(DATE_GE).append(getStartingDate(INFLUENZA));
        }
      }
    }

    // Append additional fixed parameters
    suffixBuilder
        .append(PRETTY_FALSE_PARAM)
        .append(COUNT_EQUALS)
        .append(dataRetrievalService.getBatchSize());

    // Optionally append the summary parameter
    if (summary) {
      suffixBuilder.append(SUMMARY_COUNT);
    }

    String params = suffixBuilder.toString();
    return httpMethodGet ? OBSERVATION_CALL + params : params;
  }

  public String getConditions(
      AbstractDataRetrievalService dataRetrievalService,
      Integer month,
      boolean summary,
      DataItemContext dataItemContext,
      Boolean httpMethodGet) {

    // Start building the query string for the Condition resource
    StringBuilder suffixBuilder = new StringBuilder();

    // Build the ICD code list and add date filter depending on the dataItemContext
    switch (dataItemContext) {
      case COVID -> {
        // Join COVID ICD codes
        String icdCodes = String.join(DELIMITER, dataRetrievalService.getCovidIcdCodes());
        suffixBuilder.append(CODE_PARAM).append(icdCodes);

        // Append the starting date filter for COVID conditions
        if (dataRetrievalService.getFilterResourcesByDate())
          suffixBuilder.append(RECORDED_DATE_GE).append(getStartingDate(COVID));
      }
      case INFLUENZA -> {
        // Join Influenza ICD codes
        String icdCodes = String.join(DELIMITER, dataRetrievalService.getInfluenzaIcdCodes());
        suffixBuilder.append(CODE_PARAM).append(icdCodes);

        // Append the starting date filter for Influenza conditions
        if (dataRetrievalService.getFilterResourcesByDate())
          suffixBuilder.append(RECORDED_DATE_GE).append(getStartingDate(INFLUENZA));
      }
      case KIDS_RADAR -> {
        // Join KidsRadar ICD codes
        String icdCodes = String.join(DELIMITER, dataRetrievalService.getKidsRadarIcdCodesAll());
        suffixBuilder.append(CODE_PARAM).append(icdCodes);
      }
    }
    // Append additional fixed parameters
    suffixBuilder
        .append(PRETTY_FALSE_PARAM)
        .append(COUNT_EQUALS)
        .append(dataRetrievalService.getBatchSize());

    // Optionally append the summary parameter
    if (summary) {
      suffixBuilder.append(SUMMARY_COUNT);
    }

    // Return the final constructed query string
    String params = suffixBuilder.toString();
    return Boolean.TRUE.equals(httpMethodGet) ? CONDITION_CALL + params : params;
  }

  @Override
  public String getConditions(
      AbstractDataRetrievalService fhirDataRetrievalService,
      List<String> encounterIds,
      Boolean httpMethodGet) {
    if (encounterIds == null || encounterIds.isEmpty()) {
      return httpMethodGet ? CONDITION_CALL : "";
    }
    String params = ENCOUNTER_PARAM + getListAsString(encounterIds);
    return httpMethodGet ? CONDITION_CALL + params : params;
  }

  public String getConditionsViaEncounterReference(
      AbstractDataRetrievalService fhirDataRetrievalService,
      List<String> encounterIds,
      Boolean httpMethodGet) {
    if (encounterIds == null || encounterIds.isEmpty()) {
      return httpMethodGet ? ENCOUNTER_CALL : "";
    }

    String params = ID_START_PARAM + getListAsString(encounterIds) + ENCOUNTER_INCLUDE_DIAGNOSIS;
    return httpMethodGet ? ENCOUNTER_CALL + params : params;
  }

  @Override
  public String getObservations(
      AbstractDataRetrievalService fhirDataRetrievalService,
      DataItemContext dataItemContext,
      List<String> loincCodes,
      List<String> encounterIds,
      Boolean httpMethodGet) {
    StringBuilder sb = new StringBuilder();
    // Just the get call needs the base resource parameter
    if (Boolean.TRUE.equals(httpMethodGet)) {
      sb.append(OBSERVATION_CALL);
    }
    // Encounter filter
    sb.append(ENCOUNTER_PARAM).append(getListAsString(encounterIds));
    // LOINC-Code filter
    sb.append(DELIMITER_AND).append(CODE_PARAM).append(getListAsString(loincCodes));
    // Optional: date filter
    if (fhirDataRetrievalService.getFilterResourcesByDate()) {
      switch (dataItemContext) {
        case COVID -> sb.append(DATE_GE).append(getStartingDate(COVID)).append(MIDNIGHT_TS);
        case INFLUENZA -> sb.append(DATE_GE).append(getStartingDate(INFLUENZA)).append(MIDNIGHT_TS);
        case KIDS_RADAR, KIDS_RADAR_KJP, KIDS_RADAR_PED ->
            sb.append(DATE_GE).append(getStartingDate(KIDS_RADAR)).append(MIDNIGHT_TS);
        default -> {}
      }
    }

    return sb.toString();
  }

  /**
   * Used if parameter <code>useEncounterConditionReference</code> in the {@link
   * FhirSearchConfiguration} is set on <code>true</code>.
   */
  public String getConditionsIncludingEncounter(
      AbstractDataRetrievalService dataRetrievalService,
      DataItemContext dataItemContext,
      Boolean httpMethodGet) {
    String icdCodes = "";
    switch (dataItemContext) {
      case COVID -> icdCodes = String.join(DELIMITER, dataRetrievalService.getCovidIcdCodes());
      case INFLUENZA ->
          icdCodes = String.join(DELIMITER, dataRetrievalService.getInfluenzaIcdCodes());
      case KIDS_RADAR -> {
        // Kiradar also got usage for covid and influenza codes
        List<String> allCodes = new ArrayList<>();
        allCodes.addAll(dataRetrievalService.getKidsRadarIcdCodesAll());
        allCodes.addAll(dataRetrievalService.getCovidIcdCodes());
        allCodes.addAll(dataRetrievalService.getInfluenzaIcdCodes());
        icdCodes = String.join(DELIMITER, allCodes);
      }
    }
    String params =
        CODE_PARAM
            + icdCodes
            + REV_INCLUDE_ENCOUNTER_DIAGNOSIS
            + PRETTY_FALSE_PARAM
            + COUNT_EQUALS
            + dataRetrievalService.getBatchSize();
    return httpMethodGet ? CONDITION_CALL + params : params;
  }

  @Override
  public String getPatients(
      AbstractDataRetrievalService dataRetrievalService, List<String> patientIdList) {
    return PATIENT_CALL
        + ID_START_PARAM
        + String.join(DELIMITER, patientIdList)
        + COUNT_EQUALS
        + dataRetrievalService.getBatchSize();
  }

  @Override
  public String getPatients(
      AbstractDataRetrievalService dataRetrievalService, Integer calendarYear) {
    return PATIENT_CALL
        + "birthdate=ge"
        + calendarYear
        + "-01-01"
        + "&birthdate=le"
        + calendarYear
        + "-12-31"
        + COUNT_EQUALS
        + dataRetrievalService.getBatchSize();
  }

  public String getPatientsPost(
      AbstractDataRetrievalService dataRetrievalService, List<String> patientIdList) {
    return ID_START_PARAM
        + getListAsString(patientIdList)
        + COUNT_EQUALS
        + dataRetrievalService.getBatchSize();
  }

  @Override
  public String getEncounters(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      DataItemContext dataItemContext,
      Boolean askTotal,
      String individualDateString) {
    return buildEncounterQuery(
        dataRetrievalService, patientIdList, dataItemContext, individualDateString, true, askTotal);
  }

  public String getEncountersPost(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      DataItemContext dataItemContext,
      String individualDateString) {
    return buildEncounterQuery(
        dataRetrievalService, patientIdList, dataItemContext, individualDateString, false, false);
  }

  private String buildEncounterQuery(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      DataItemContext dataItemContext,
      String individualDateString,
      boolean includePrefix,
      boolean askTotal) {

    StringBuilder suffixBuilder = new StringBuilder();

    if (includePrefix) {
      suffixBuilder.append(ENCOUNTER_CALL);
      suffixBuilder.append(SUBJECT_PARAM).append(String.join(DELIMITER, patientIdList));
    } else {
      suffixBuilder.append(SUBJECT_PARAM).append(getListAsString(patientIdList));
    }

    if (dataRetrievalService.getFilterResourcesByDate()) {
      switch (dataItemContext) {
        case COVID ->
            suffixBuilder.append(DATE_GE).append(getStartingDate(COVID)).append(MIDNIGHT_TS);
        case INFLUENZA ->
            suffixBuilder.append(DATE_GE).append(getStartingDate(INFLUENZA)).append(MIDNIGHT_TS);
        case KIDS_RADAR ->
            suffixBuilder.append(DATE_GE).append(getStartingDate(KIDS_RADAR)).append(MIDNIGHT_TS);
        case ACRIBIS ->
            suffixBuilder.append(DATE_GE).append(individualDateString).append(MIDNIGHT_TS);
      }
    }
    suffixBuilder.append(COUNT_EQUALS).append(dataRetrievalService.getBatchSize());

    if (includePrefix && askTotal) {
      suffixBuilder.append(SUMMARY_COUNT);
    }

    return suffixBuilder.toString();
  }

  @Override
  public String getProcedures(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      List<String> encounterIdList,
      String systemUrl,
      Boolean askTotal,
      List<String> wards,
      DataItemContext dataItemContext) {

    List<String> params =
        buildProcedureQueryParams(
            dataRetrievalService,
            patientIdList,
            encounterIdList,
            systemUrl,
            dataItemContext,
            askTotal);

    return "Procedure?" + String.join(DELIMITER_AND, params);
  }

  public String getProceduresPost(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      List<String> encounterIdList,
      String systemUrl,
      DataItemContext dataItemContext) {

    List<String> params =
        buildProcedureQueryParams(
            dataRetrievalService,
            patientIdList,
            encounterIdList,
            systemUrl,
            dataItemContext,
            false); // askTotal not relevant for post queries

    return String.join(DELIMITER_AND, params);
  }

  @Override
  public String getIcuProcedures(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> wards,
      Collection<String> encounterIds,
      boolean clapp) {
    return "";
  }

  public String getProcedures(
      AbstractDataRetrievalService dataRetrievalService,
      DataItemContext dataItemContext,
      List<String> encounterIds,
      String systemUrl,
      Boolean httpMethodGet,
      Boolean askTotal) {

    StringBuilder sb = new StringBuilder();
    if (Boolean.TRUE.equals(httpMethodGet)) {
      sb.append(PROCEDURE_CALL);
    }
    List<String> params =
        buildProcedureQueryParams(
            dataRetrievalService, null, encounterIds, systemUrl, dataItemContext, askTotal);
    sb.append(String.join(DELIMITER_AND, params));
    return sb.toString();
  }

  private List<String> buildProcedureQueryParams(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      List<String> encounterIdList,
      String systemUrl,
      DataItemContext dataItemContext,
      Boolean askTotal) {

    List<String> params = new ArrayList<>();

    if (patientIdList != null && !patientIdList.isEmpty()) {
      params.addFirst(PATIENT_PARAM + getListAsString(patientIdList));
    } else if (encounterIdList != null && !encounterIdList.isEmpty()) {
      params.addFirst(ENCOUNTER_PARAM + getListAsString(encounterIdList));
    }
    params.add(COUNT_EQUALS_PARAM + dataRetrievalService.getBatchSize());
    switch (dataItemContext) {
      case COVID, INFLUENZA:
        params.add(
            CODE_PARAM
                + getProcedureCodesAsString(
                    dataRetrievalService, dataItemContext, TerminologySystems.SNOMED));
    }
    if (Boolean.TRUE.equals(askTotal)) {
      params.add(SUMMARY_COUNT_PARAM);
    }

    return params;
  }

  /**
   * Retrieves procedure codes for ventilation and ECMO from the given data retrieval service,
   * formats them by prefixing each code with the system URL and a delimiter, and returns the
   * concatenated result as a single string.
   *
   * <p>The prefix is needed to boost queries on blaze fhir server.
   *
   * @param dataRetrievalService an instance of {@link AbstractDataRetrievalService} used to fetch
   *     ventilation and ECMO procedure codes.
   * @param systemUrl the URL prefix to be added to each procedure code.
   * @return a concatenated string of formatted procedure codes, with ventilation codes followed by
   *     ECMO codes, separated by the defined delimiter {@code DELIMITER}.
   */
  private static String getProcedureCodesAsString(
      AbstractDataRetrievalService dataRetrievalService,
      DataItemContext dataItemContext,
      String systemUrl) {
    Function<String, String> addSystemUrlPrefix = code -> systemUrl + PIPE + code;

    String ventilationCodes =
        dataRetrievalService.getProcedureVentilationCodes().stream()
            .map(addSystemUrlPrefix)
            .collect(Collectors.joining(DELIMITER));

    String ecmoCodes =
        dataRetrievalService.getProcedureEcmoCodes().stream()
            .map(addSystemUrlPrefix)
            .collect(Collectors.joining(DELIMITER));

    String highFlowCodes =
        dataRetrievalService.getProcedureHighFlowCodes().stream()
            .map(addSystemUrlPrefix)
            .collect(Collectors.joining(DELIMITER));

    String cpapCodes =
        dataRetrievalService.getProcedureCpapCodes().stream()
            .map(addSystemUrlPrefix)
            .collect(Collectors.joining(DELIMITER));

    return switch (dataItemContext) {
      case COVID, INFLUENZA -> ventilationCodes + DELIMITER + ecmoCodes;
      case KIDS_RADAR, KIDS_RADAR_PED ->
          ventilationCodes
              + DELIMITER
              + ecmoCodes
              + DELIMITER
              + highFlowCodes
              + DELIMITER
              + cpapCodes;
      default -> null;
    };
  }

  public static List<String> getIcuProcedureCodesAsList(
      AbstractDataRetrievalService dataRetrievalService, DataItemContext dataItemContext) {

    // Retrieve code lists directly (no prefix)
    List<String> ventilationCodes = dataRetrievalService.getProcedureVentilationCodes();
    List<String> ecmoCodes = dataRetrievalService.getProcedureEcmoCodes();
    List<String> highFlowCodes = dataRetrievalService.getProcedureHighFlowCodes();
    List<String> cpapCodes = dataRetrievalService.getProcedureCpapCodes();

    // Combine lists depending on the dataItemContext
    return switch (dataItemContext) {
      case COVID, INFLUENZA ->
          Stream.concat(ventilationCodes.stream(), ecmoCodes.stream()).collect(Collectors.toList());
      case KIDS_RADAR, KIDS_RADAR_PED ->
          Stream.of(
                  ventilationCodes.stream(),
                  ecmoCodes.stream(),
                  highFlowCodes.stream(),
                  cpapCodes.stream())
              .flatMap(Function.identity())
              .collect(Collectors.toList());
      default -> Collections.emptyList();
    };
  }

  @Override
  public String getLocations(
      AbstractDataRetrievalService dataRetrievalService,
      List<?> locationIdList,
      Boolean httpMethodGet) {

    if (locationIdList == null || locationIdList.isEmpty()) {
      throw new IllegalArgumentException("locationIdList must not be null or empty");
    }

    String ids = getListAsString(locationIdList);

    StringBuilder sb = new StringBuilder();
    if (Boolean.TRUE.equals(httpMethodGet)) {
      sb.append(LOCATION_CALL);
    }
    sb.append(ID_START_PARAM)
        .append(ids)
        .append(COUNT_EQUALS)
        .append(dataRetrievalService.getBatchSize());

    return sb.toString();
  }

  @Override
  public String getConsents(
      AbstractDataRetrievalService dataRetrievalService, DataItemContext dataItemContext) {
    return "Consent?category="
        // supporting former category systems to make it backwards compatible
        + CONSENT_CATEGORY_SYSTEM
        + PIPE
        + CONSENT_CATEGORY_CODE
        + ","
        + CONSENT_CATEGORY_SYSTEM_2026
        + PIPE
        + CONSENT_CATEGORY_CODE
        + DATE_GE
        + getStartingDate(dataItemContext);
  }

  @Override
  public String getQuestionnaireResponses(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      Collection<String> questionnaireIds,
      Boolean httpMethodGet) {

    if (patientIdList == null || patientIdList.isEmpty()) {
      throw new IllegalArgumentException("patientIdList must not be null or empty");
    }

    String ids = getListAsString(patientIdList);
    String questionnaireReferences = getListAsString(questionnaireIds);
    // Search param needs canonical URL
    String questionnaireUrl =
        "https://www.medizininformatik-initiative.de/fhir/ext/modul-kardio/Questionnaire/MII-QN-Kardio-Acribis-Study-FollowUp";

    StringBuilder sb = new StringBuilder();
    if (Boolean.TRUE.equals(httpMethodGet)) {
      sb.append(QUESTIONNAIRE_RESPONSE_CALL);
    }
    sb.append(SUBJECT_PARAM)
        .append(ids)
        .append(DELIMITER_AND)
        .append(QUESTIONNAIRE_PARAM)
        .append(questionnaireUrl)
        .append(COUNT_EQUALS)
        .append(dataRetrievalService.getBatchSize());
    return sb.toString();
  }

  @Override
  public String getIcuEncounters(
      AbstractDataRetrievalService dataRetrievalService, Integer calendarYear) {
    return null;
  }

  @Override
  public String getIcuEpisodes(
      AbstractDataRetrievalService dataRetrievalService, List<String> encounterIdList) {
    return null;
  }

  @Override
  public String getUkbRenalReplacementObservations(
      AbstractDataRetrievalService abstractRestConfiguration,
      List<String> encounterIdList,
      Set<Integer> orbisCodes) {
    return null;
  }

  @Override
  public String getUkbRenalReplacementBodyWeight(
      AcuwaveDataRetrievalService acuwaveDataRetrievalService,
      List<String> encounterIdSublist,
      DataSourceType dataSourceType) {
    return null;
  }

  @Override
  public String getUkbRenalReplacementStart(
      AcuwaveDataRetrievalService acuwaveDataRetrievalService,
      List<String> encounterIdSublist,
      DataSourceType dataSourceType) {
    return null;
  }

  @Override
  public String getUkbRenalReplacementUrineOutput(
      AcuwaveDataRetrievalService acuwaveDataRetrievalService,
      List<String> encounterIdSublist,
      DataSourceType dataSourceType) {
    return null;
  }

  @Override
  public String getQuestionnaires(
      FhirDataRetrievalService fhirDataRetrievalService, boolean useGet) {
    return QUESTIONNAIRE_CALL + URL_PARAM + QUESTIONNAIRE_RESOURCE_URL;
  }

  public String getStatus(AbstractDataRetrievalService dataRetrievalService) {
    return "metadata";
  }
}
