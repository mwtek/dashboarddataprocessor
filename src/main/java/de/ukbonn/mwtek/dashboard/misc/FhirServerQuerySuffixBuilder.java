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

import de.ukbonn.mwtek.dashboard.configuration.FhirSearchConfiguration;
import de.ukbonn.mwtek.dashboard.interfaces.DataSourceType;
import de.ukbonn.mwtek.dashboard.interfaces.QuerySuffixBuilder;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.AcuwaveDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/** Building the templates of the individual REST requests to the Acuwaveles server. */
public class FhirServerQuerySuffixBuilder implements QuerySuffixBuilder {

  private static final String COUNT_EQUALS = "&_count=";
  private static final String DELIMITER = ",";
  public static final String SUMMARY_COUNT = "&_summary=count";
  public static final String DATE_GE = "&date=ge";
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
  public static final String SUBJECT_PARAM = "subject=";
  public static final String ENCOUNTER_CALL = "Encounter?";

  public String getObservations(
      AbstractDataRetrievalService dataRetrievalService,
      Integer month,
      boolean summary,
      DataItemContext dataItemContext) {

    // Start building the query string for the Observation resource
    StringBuilder suffixBuilder = new StringBuilder(OBSERVATION_CALL);

    // Build query parameters based on the dataItemContext
    switch (dataItemContext) {
      case COVID -> {
        // Join COVID PCR and Variant codes
        String labPcrCodes = String.join(DELIMITER, dataRetrievalService.getCovidLabPcrCodes());
        String labVariantCodes =
            String.join(DELIMITER, dataRetrievalService.getCovidLabVariantCodes());

        // Append the codes as a query parameter
        suffixBuilder.append(CODE_PARAM).append(labPcrCodes);
        if (!labVariantCodes.isBlank()) {
          suffixBuilder.append(DELIMITER).append(labVariantCodes);
        }

        // Append the starting date filter for COVID observations
        if (dataRetrievalService.getFilterResourcesByDate())
          suffixBuilder.append(DATE_GE).append(getStartingDate(COVID));
      }
      case INFLUENZA -> {
        // Join Influenza PCR codes
        String labPcrCodes = String.join(DELIMITER, dataRetrievalService.getInfluenzaLabPcrCodes());

        // Append the codes as a query parameter
        suffixBuilder.append(CODE_PARAM).append(labPcrCodes);

        // Append the starting date filter for Influenza observations
        if (dataRetrievalService.getFilterResourcesByDate())
          suffixBuilder.append(DATE_GE).append(getStartingDate(INFLUENZA));
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
    return suffixBuilder.toString();
  }

  public String getConditions(
      AbstractDataRetrievalService dataRetrievalService,
      Integer month,
      boolean summary,
      DataItemContext dataItemContext) {

    // Start building the query string for the Condition resource
    StringBuilder suffixBuilder = new StringBuilder(CONDITION_CALL);

    // Build the ICD code list and add date filter depending on the dataItemContext
    switch (dataItemContext) {
      case COVID -> {
        // Join COVID ICD codes
        String icdCodes = String.join(DELIMITER, dataRetrievalService.getCovidIcdCodes());
        suffixBuilder.append(CODE_PARAM).append(icdCodes);

        // Append the starting date filter for COVID conditions
        if (dataRetrievalService.getFilterResourcesByDate())
          suffixBuilder.append(DATE_GE).append(getStartingDate(COVID));
      }
      case INFLUENZA -> {
        // Join Influenza ICD codes
        String icdCodes = String.join(DELIMITER, dataRetrievalService.getInfluenzaIcdCodes());
        suffixBuilder.append(CODE_PARAM).append(icdCodes);

        // Append the starting date filter for Influenza conditions
        if (dataRetrievalService.getFilterResourcesByDate())
          suffixBuilder.append(DATE_GE).append(getStartingDate(INFLUENZA));
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
    return suffixBuilder.toString();
  }

  @Override
  public String getConditions(
      AbstractDataRetrievalService fhirDataRetrievalService, List<String> encounterIds) {

    // Start building the query string for the Condition resource
    StringBuilder suffixBuilder = new StringBuilder(CONDITION_CALL);
    suffixBuilder.append(ENCOUNTER_PARAM).append(getListAsString(encounterIds));

    return suffixBuilder.toString();
  }

  public String getConditionsPost(
      AbstractDataRetrievalService fhirDataRetrievalService, List<String> encounterIds) {

    // Start building the query string for the Condition resource
    StringBuilder suffixBuilder = new StringBuilder();
    suffixBuilder.append(ENCOUNTER_PARAM).append(getListAsString(encounterIds));

    return suffixBuilder.toString();
  }

  /**
   * Used if parameter <code>useEncounterConditionReference</code> in the {@link
   * FhirSearchConfiguration} is set on <code>true</code>.
   */
  public String getConditionsIncludingEncounter(
      AbstractDataRetrievalService dataRetrievalService, DataItemContext dataItemContext) {
    String icdCodes = "";
    switch (dataItemContext) {
      case COVID -> icdCodes = String.join(DELIMITER, dataRetrievalService.getCovidIcdCodes());
      case INFLUENZA ->
          icdCodes = String.join(DELIMITER, dataRetrievalService.getInfluenzaIcdCodes());
      case KIDS_RADAR ->
          icdCodes = String.join(DELIMITER, dataRetrievalService.getKidsRadarIcdCodesAll());
    }
    return "Condition?code="
        + icdCodes
        + "&_revinclude=Encounter:diagnosis&_pretty=false"
        + COUNT_EQUALS
        + dataRetrievalService.getBatchSize();
  }

  @Override
  public String getPatients(
      AbstractDataRetrievalService dataRetrievalService, List<String> patientIdList) {
    return "Patient?_id="
        + String.join(DELIMITER, patientIdList)
        + COUNT_EQUALS
        + dataRetrievalService.getBatchSize();
  }

  public String getPatientsPost(
      AbstractDataRetrievalService dataRetrievalService, List<String> patientIdList) {
    return "_id="
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
    // Filtering of case class will be done in post-processing since it's way faster
    //    if (dataItemContext == ACRIBIS || dataItemContext == KIDS_RADAR) {
    //      suffixBuilder.append(CLASS_IMP);
    //    }

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

  private List<String> buildProcedureQueryParams(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      List<String> encounterIdList,
      String systemUrl,
      DataItemContext dataItemContext,
      Boolean askTotal) {

    List<String> params = new ArrayList<>();

    String procedureCodes;
    switch (dataItemContext) {
      case COVID, INFLUENZA:
        procedureCodes = getProcedureCodesAsString(dataRetrievalService, systemUrl);
        if (!procedureCodes.isEmpty()) {
          params.add(CODE_PARAM + procedureCodes);
        }
    }

    if (patientIdList != null && !patientIdList.isEmpty()) {
      params.add(0, PATIENT_PARAM + getListAsString(patientIdList));
    } else if (encounterIdList != null && !encounterIdList.isEmpty()) {
      params.add(0, ENCOUNTER_PARAM + getListAsString(encounterIdList));
    }

    params.add(COUNT_EQUALS + dataRetrievalService.getBatchSize());

    if (Boolean.TRUE.equals(askTotal)) {
      params.add(SUMMARY_COUNT);
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
      AbstractDataRetrievalService dataRetrievalService, String systemUrl) {
    Function<String, String> addSystemUrlPrefix = code -> systemUrl + PIPE + code;

    String ventilationCodes =
        dataRetrievalService.getProcedureVentilationCodes().stream()
            .map(addSystemUrlPrefix)
            .collect(Collectors.joining(DELIMITER));

    String ecmoCodes =
        dataRetrievalService.getProcedureEcmoCodes().stream()
            .map(addSystemUrlPrefix)
            .collect(Collectors.joining(DELIMITER));

    return ventilationCodes + DELIMITER + ecmoCodes;
  }

  @Override
  public String getLocations(
      AbstractDataRetrievalService dataRetrievalService, List<?> locationIdList) {
    return "Location?_id="
        + StringUtils.join(locationIdList, ',')
        + COUNT_EQUALS
        + dataRetrievalService.getBatchSize();
  }

  @Override
  public String getConsents(
      AbstractDataRetrievalService dataRetrievalService, DataItemContext dataItemContext) {
    return "Consent?category="
        + CONSENT_CATEGORY_SYSTEM
        + PIPE
        + CONSENT_CATEGORY_CODE
        + DATE_GE
        + getStartingDate(ACRIBIS);
  }

  @Override
  public String getLocationsPost(
      AbstractDataRetrievalService dataRetrievalService, List<?> locationIdList) {
    return "_id="
        + getListAsString(locationIdList)
        + COUNT_EQUALS
        + dataRetrievalService.getBatchSize();
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

  public String getStatus(AbstractDataRetrievalService dataRetrievalService) {
    return "metadata";
  }
}
