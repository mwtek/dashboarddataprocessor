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
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.Covid;
import de.ukbonn.mwtek.dashboardlogic.enums.NumDashboardConstants.KidsRadar;
import java.util.List;
import java.util.Set;

/** Building the templates of the individual REST requests to the Acuwaveles server. */
public interface QuerySuffixBuilder {

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation}
   * resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param month The calendar month for which data is requested. (for parallelization).
   * @param summary Should only the bundle with the total number of found resources be output
   *     instead of the data retrieval query?
   * @return A list of all FHIR observation resources that include a covid finding.
   */
  String getObservations(
      AbstractDataRetrievalService dataRetrievalService,
      Integer month,
      boolean summary,
      DataItemContext dataItemContext);

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition} resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param month The calendar month for which data is requested. (for parallelization)
   * @param summary Should only the bundle with the total number of found resources be output
   *     instead of the data retrieval query?
   * @return A list of all FHIR condition resources that include a covid diagnosis.
   */
  String getConditions(
      AbstractDataRetrievalService dataRetrievalService,
      Integer month,
      boolean summary,
      DataItemContext dataItemContext);

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient} resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param patientIdList A list with patient ids used as input criteria.
   * @return A list of all requested FHIR patient resources.
   */
  String getPatients(AbstractDataRetrievalService dataRetrievalService, List<String> patientIdList);

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter} resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param patientIdList A list with patient ids used as input criteria.
   * @return A list of all requested FHIR encounter resources.
   */
  String getEncounters(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      DataItemContext dataItemContext,
      Boolean askTotal);

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure} resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param encounterIdList A list with encounter ids used as input criteria.
   * @return A list of all requested FHIR procedure resources that contain artificial ventilation
   *     data.
   */
  String getProcedures(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> encounterIdList,
      Boolean askTotal);

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation} resources.
   *
   * @param dataRetrievalService The corresponding data search service.
   * @param locationIdList A list with location ids used as input criteria.
   * @return A list of all requested FHIR location resources.
   */
  String getLocations(AbstractDataRetrievalService dataRetrievalService, List<?> locationIdList);

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
        return Covid.QUALIFYING_YEAR
            + "-"
            + addLeadingZeroIfNeeded(Covid.QUALIFYING_MONTH)
            + "-"
            + addLeadingZeroIfNeeded(Covid.QUALIFYING_DAY);
      }
      case KIDS_RADAR -> {
        return KidsRadar.QUALIFYING_YEAR
            + "-"
            + addLeadingZeroIfNeeded(KidsRadar.QUALIFYING_MONTH)
            + "-"
            + addLeadingZeroIfNeeded(KidsRadar.QUALIFYING_DAY);
      }
    }
    // Default
    return "2020-01-27";
  }

  private static String addLeadingZeroIfNeeded(int qualifyingMonthKidsRadar) {
    return String.format("%02d", qualifyingMonthKidsRadar);
  }
}
