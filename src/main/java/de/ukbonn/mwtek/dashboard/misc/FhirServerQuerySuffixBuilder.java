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

import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.COVID;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.INFLUENZA;
import static de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext.KIDS_RADAR;

import de.ukbonn.mwtek.dashboard.configuration.FhirSearchConfiguration;
import de.ukbonn.mwtek.dashboard.interfaces.DataSourceType;
import de.ukbonn.mwtek.dashboard.interfaces.QuerySuffixBuilder;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.AcuwaveDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/** Building the templates of the individual REST requests to the Acuwaveles server. */
public class FhirServerQuerySuffixBuilder implements QuerySuffixBuilder {

  private static final String COUNT_EQUALS = "&_count=";
  private static final String DELIMITER = ",";

  public String getObservations(
      AbstractDataRetrievalService dataRetrievalService,
      Integer month,
      boolean summary,
      DataItemContext dataItemContext) {
    String labPcrCodes = "";
    String labVariantCodes = "";
    switch (dataItemContext) {
      case COVID -> {
        labPcrCodes = String.join(DELIMITER, dataRetrievalService.getCovidLabPcrCodes());
        labVariantCodes = String.join(DELIMITER, dataRetrievalService.getCovidLabVariantCodes());
      }
      case INFLUENZA -> {
        labPcrCodes = String.join(DELIMITER, dataRetrievalService.getInfluenzaLabPcrCodes());
      }
    }
    return "Observation?code="
        + labPcrCodes
        + (!labVariantCodes.isBlank() ? (DELIMITER + labVariantCodes) : "")
        + "&_pretty=false"
        + COUNT_EQUALS
        + dataRetrievalService.getBatchSize()
        + (summary ? "&_summary=count" : "");
  }

  public String getConditions(
      AbstractDataRetrievalService dataRetrievalService,
      Integer month,
      boolean summary,
      DataItemContext dataItemContext) {
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
        + "&_pretty=false"
        + COUNT_EQUALS
        + dataRetrievalService.getBatchSize()
        + (summary ? "&_summary=count" : "");
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

  public String getPatients(
      AbstractDataRetrievalService dataRetrievalService, List<String> patientIdList) {
    return "Patient?_id="
        + String.join(DELIMITER, patientIdList)
        + COUNT_EQUALS
        + dataRetrievalService.getBatchSize();
  }

  @Override
  public String getEncounters(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      DataItemContext dataItemContext,
      Boolean askTotal) {
    StringBuilder suffixBuilder = new StringBuilder();
    suffixBuilder.append("Encounter?subject=").append(String.join(DELIMITER, patientIdList));

    /* For this project, theoretically only cases with an intake date after a cut-off date
    (27/01/2020) are needed. To reduce the resource results and make the queries more
     streamlined, a "&location-period=gt2020-27-01" is added on demand to the fhir search, as
     we cannot assume that every location stores the transfer history in the Encounter
     resource.*/
    if (dataRetrievalService.getFilterEncounterByDate()) {
      switch (dataItemContext) {
        case COVID -> suffixBuilder.append("&date=gt").append(getStartingDate(COVID));
        case INFLUENZA -> suffixBuilder.append("&date=gt").append(getStartingDate(INFLUENZA));
        case KIDS_RADAR ->
            suffixBuilder
                .append("&date=gt")
                .append(getStartingDate(KIDS_RADAR))
                .append("&_class=IMP");
      }
    }
    suffixBuilder.append(COUNT_EQUALS).append(dataRetrievalService.getBatchSize());

    if (askTotal) {
      suffixBuilder.append("&_summary=count");
    }
    return suffixBuilder.toString();
  }

  @Override
  public String getProcedures(
      AbstractDataRetrievalService dataRetrievalService,
      List<String> patientIdList,
      Boolean askTotal) {
    return "Procedure?code="
        + String.join(DELIMITER, dataRetrievalService.getProcedureVentilationCodes())
        + DELIMITER
        + String.join(DELIMITER, dataRetrievalService.getProcedureEcmoCodes())
        + "&subject="
        + StringUtils.join(patientIdList, ',')
        + COUNT_EQUALS
        + dataRetrievalService.getBatchSize()
        + (askTotal ? "&_summary=count" : "");
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
}
