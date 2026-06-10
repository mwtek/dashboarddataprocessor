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
package de.ukbonn.mwtek.dashboard.interfaces;

import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.AcuwaveDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.FhirDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.Acribis;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.Bct;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.Covid;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.Influenza;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.KidsRadar;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Building the templates of the individual REST requests to the Acuwaveles server. */
public interface QuerySuffixBuilder {

  /**
   * The retrieval of FHIR {@link MiiObservation} resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param month The calendar month for which data is requested. (for parallelization).
   * @param summary Should only the bundle with the total number of found resources be output
   *     instead of the data retrieval query?
   * @param httpMethodGet Is {@link org.springframework.http.HttpMethod#GET} used?
   * @return A list of all FHIR observation resources that include a covid finding.
   */
  String getObservations(
      AbstractDataRetrievalService dataRetrievalService,
      Integer month,
      boolean summary,
      DataItemContext dataItemContext,
      boolean httpMethodGet);

  /**
   * The retrieval of FHIR {@link MiiCondition} resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param month The calendar month for which data is requested. (for parallelization)
   * @param summary Should only the bundle with the total number of found resources be output
   *     instead of the data retrieval query?
   * @param httpMethodGet Is {@link org.springframework.http.HttpMethod#GET} used?
   * @return A list of all FHIR condition resources that include a covid diagnosis.
   */
  String getConditions(
      AbstractDataRetrievalService dataRetrievalService,
      Integer month,
      boolean summary,
      DataItemContext dataItemContext,
      Boolean httpMethodGet);

  String getConditions(
      AbstractDataRetrievalService acuwaveDataRetrievalService,
      List<String> encounterIds,
      Boolean httpMethodGet);

  String getObservations(
      AbstractDataRetrievalService fhirDataRetrievalService,
      DataItemContext dataItemContext,
      List<String> loincCodes,
      List<String> encounterIds,
      Boolean httpMethodGet);

  /**
   * The retrieval of FHIR {@link MiiPatient} resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param patientIdList A list with patient ids used as input criteria.
   * @return A list of all requested FHIR patient resources.
   */
  String getPatients(AbstractDataRetrievalService dataRetrievalService, List<String> patientIdList);

  /**
   * The retrieval of FHIR {@link MiiPatient} resources with birthdate in the given calendar year.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param calendarYear The calendar year in which the date of birth must fall.
   * @return A list of all requested FHIR patient resources.
   */
  String getPatients(AbstractDataRetrievalService dataRetrievalService, Integer calendarYear);

  /**
   * The retrieval of FHIR {@link MiiEncounter} resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param patientIdList A list with patient ids used as input criteria.
   * @return A list of all requested FHIR encounter resources.
   */
  String getEncounters(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      DataItemContext dataItemContext,
      Boolean askTotal,
      String individualDateString);

  /**
   * The retrieval of FHIR {@link MiiProcedure} resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param encounterIdList A list with encounter ids used as input criteria.
   * @return A list of all requested FHIR procedure resources that contain artificial ventilation
   *     data.
   */
  String getProcedures(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      List<String> encounterIdList,
      String systemUrl,
      Boolean askTotal,
      List<String> wards,
      DataItemContext dataItemContext);

  /**
   * The retrieval of FHIR {@link MiiProcedure} resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param encounterIdList A list with encounter ids used as input criteria.
   * @return A list of all requested FHIR procedure resources that contain artificial ventilation
   *     data.
   */
  String getProceduresPost(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      List<String> encounterIdList,
      String systemUrl,
      DataItemContext dataItemContext);

  String getIcuProcedures(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> wards,
      Collection<String> encounterIds,
      boolean clapp);

  /**
   * The retrieval of FHIR {@link MiiLocation} resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param locationIdList A list with location ids used as input criteria.
   * @return A list of all requested FHIR location resources.
   */
  String getLocations(
      AbstractDataRetrievalService dataRetrievalService,
      List<?> locationIdList,
      Boolean httpMethodGet);

  String getConsents(
      AbstractDataRetrievalService dataRetrievalService, DataItemContext dataItemContext);

  String getQuestionnaireResponses(
      AbstractDataRetrievalService abstractRestConfiguration,
      List<String> encounterIdList,
      Collection<String> questionnaireIds,
      Boolean httpMethodGet);

  String getIcuEncounters(AbstractDataRetrievalService dataRetrievalService, Integer calendarYear);

  public String getIcuEpisodes(
      AbstractDataRetrievalService abstractRestConfiguration, List<String> encounterIdList);

  String getUkbRenalReplacementObservations(
      AbstractDataRetrievalService abstractRestConfiguration,
      List<String> encounterIdList,
      Set<Integer> orbisCodes);

  String getUkbRenalReplacementBodyWeight(
      AcuwaveDataRetrievalService acuwaveDataRetrievalService,
      List<String> encounterIdSublist,
      DataSourceType dataSourceType);

  String getUkbRenalReplacementStart(
      AcuwaveDataRetrievalService acuwaveDataRetrievalService,
      List<String> encounterIdSublist,
      DataSourceType dataSourceType);

  String getUkbRenalReplacementUrineOutput(
      AcuwaveDataRetrievalService acuwaveDataRetrievalService,
      List<String> encounterIdSublist,
      DataSourceType dataSourceType);

  default String getStartingDate(DataItemContext dataItemContext) {
    switch (dataItemContext) {
      case COVID -> {
        return Covid.QUALIFYING_YEAR
            + "-"
            + addLeadingZeroIfNeeded(Covid.QUALIFYING_MONTH)
            + "-"
            + addLeadingZeroIfNeeded(Covid.QUALIFYING_DAY);
      }
      case INFLUENZA -> {
        return Influenza.QUALIFYING_YEAR
            + "-"
            + addLeadingZeroIfNeeded(Influenza.QUALIFYING_MONTH)
            + "-"
            + addLeadingZeroIfNeeded(Influenza.QUALIFYING_DAY);
      }
      case KIDS_RADAR, KIDS_RADAR_PED -> {
        return KidsRadar.QUALIFYING_YEAR
            + "-"
            + addLeadingZeroIfNeeded(KidsRadar.QUALIFYING_MONTH)
            + "-"
            + addLeadingZeroIfNeeded(KidsRadar.QUALIFYING_DAY);
      }
      case ACRIBIS -> {
        return Acribis.QUALIFYING_YEAR
            + "-"
            + addLeadingZeroIfNeeded(Acribis.QUALIFYING_MONTH)
            + "-"
            + addLeadingZeroIfNeeded(Acribis.QUALIFYING_DAY);
      }
      case BCT -> {
        return Bct.QUALIFYING_YEAR
            + "-"
            + addLeadingZeroIfNeeded(Bct.QUALIFYING_MONTH)
            + "-"
            + addLeadingZeroIfNeeded(Bct.QUALIFYING_DAY);
      }
    }
    // Default
    return "2020-01-27";
  }

  private static String addLeadingZeroIfNeeded(int qualifyingMonthKidsRadar) {
    return String.format("%02d", qualifyingMonthKidsRadar);
  }

  String getQuestionnaires(FhirDataRetrievalService fhirDataRetrievalService, boolean useGet);
}
