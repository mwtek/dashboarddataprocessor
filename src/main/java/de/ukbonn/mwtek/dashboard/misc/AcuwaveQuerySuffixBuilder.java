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

import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.getKickOffDateStringFormat;

import de.ukbonn.mwtek.dashboard.enums.AcuwaveDataSourceType;
import de.ukbonn.mwtek.dashboard.interfaces.DataSourceType;
import de.ukbonn.mwtek.dashboard.interfaces.QuerySuffixBuilder;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import de.ukbonn.mwtek.dashboard.services.AcuwaveDataRetrievalService;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Building the templates of the individual REST requests to the Acuwaveles server. */
public class AcuwaveQuerySuffixBuilder implements QuerySuffixBuilder {

  public static final String DELIMITER = ",";

  public String getObservations(
      AbstractDataRetrievalService acuwaveDataRetrievalService,
      Integer month,
      boolean summary,
      DataItemContext dataItemContext) {
    String orbisLabPcrCodes = "";
    String orbisLabVariantCodes = "";
    switch (dataItemContext) {
      case COVID -> {
        orbisLabPcrCodes =
            ((AcuwaveDataRetrievalService) acuwaveDataRetrievalService)
                .getCovidOrbisLabPcrCodes().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(DELIMITER));
        orbisLabVariantCodes =
            ((AcuwaveDataRetrievalService) acuwaveDataRetrievalService)
                .getCovidOrbisLabVariantCodes().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(DELIMITER));
      }
      case INFLUENZA ->
          orbisLabPcrCodes =
              ((AcuwaveDataRetrievalService) acuwaveDataRetrievalService)
                  .getInfluenzaOrbisLabPcrCodes().stream()
                      .map(String::valueOf)
                      .collect(Collectors.joining(DELIMITER));
    }
    return "kdslabor?codes="
        + orbisLabPcrCodes
        + (!orbisLabVariantCodes.isBlank() ? DELIMITER + orbisLabVariantCodes : "")
        + "&months="
        + month
        + "&hideResourceTypes=ServiceRequest,DiagnosticReport";
  }

  public String getConditions(
      AbstractDataRetrievalService acuwaveDataRetrievalService,
      Integer month,
      boolean summary,
      DataItemContext dataItemContext) {
    String icdCodes = "";
    switch (dataItemContext) {
      case COVID ->
          icdCodes = String.join(DELIMITER, acuwaveDataRetrievalService.getCovidIcdCodes());
      case INFLUENZA ->
          icdCodes = String.join(DELIMITER, acuwaveDataRetrievalService.getInfluenzaIcdCodes());
      case KIDS_RADAR ->
          icdCodes = String.join(DELIMITER, acuwaveDataRetrievalService.getKidsRadarIcdCodesAll());
    }
    return "kdsdiagnose?codes="
        + icdCodes
        + "&months="
        + month
        + "&reportDateFrom="
        + getKickOffDateStringFormat(dataItemContext);
  }

  @Override
  public String getPatients(
      AbstractDataRetrievalService abstractRestConfiguration, List<String> patientIdList) {
    return "kdsperson?patients="
        + String.join(DELIMITER, patientIdList)
        + "&hideResourceTypes=Observation";
  }

  @Override
  public String getEncounters(
      AbstractDataRetrievalService abstractRestConfiguration,
      List<String> patientIdList,
      DataItemContext dataItemContext,
      Boolean askTotal) {
    switch (dataItemContext) {
      case COVID, INFLUENZA -> {
        return "kdsfall?patients="
            + String.join(DELIMITER, patientIdList)
            + "&admissionDateFrom="
            + getStartingDate(dataItemContext);
      }
      case KIDS_RADAR -> {
        return "kdsfall?patients="
            + String.join(DELIMITER, patientIdList)
            + "&admissionDateFrom="
            + getStartingDate(dataItemContext)
            + "&classes=ST";
      }
    }
    return null;
  }

  @Override
  public String getProcedures(
      AbstractDataRetrievalService abstractRestConfiguration,
      List<String> encounterIdList,
      Boolean askTotal) {
    return "kdsicu?cases=" + String.join(DELIMITER, encounterIdList) + "&skipDuplicateCheck=true";
  }

  @Override
  public String getLocations(
      AbstractDataRetrievalService abstractRestConfiguration, List<?> locationIdSublist) {
    return "location?ids="
        + locationIdSublist.stream().map(String::valueOf).collect(Collectors.joining(DELIMITER));
  }

  @Override
  public String getIcuEncounters(
      AbstractDataRetrievalService abstractRestConfiguration, Integer calendarYear) {
    return "kdsfall?admissionDateFrom="
        + calendarYear
        + "-01-01&admissionDateTo="
        + calendarYear
        + "-12-31&icuOnly=true&classes=st,post";
  }

  public String getEncountersDebug(
      AbstractDataRetrievalService abstractRestConfiguration, List<String> encounterIdList) {
    return "kdsfall?cases="
        + String.join(DELIMITER, encounterIdList)
        + "&episodeSystems=PDMSREPORTING";
  }

  @Override
  public String getIcuEpisodes(
      AbstractDataRetrievalService abstractRestConfiguration, List<String> encounterIdList) {
    return "kdsfall?&cases="
        + String.join(DELIMITER, encounterIdList)
        + "&episodeSystems=PDMSREPORTING&episodeSystemsWards=01A,03A,03B,04A";
  }

  @Override
  public String getUkbRenalReplacementObservations(
      AbstractDataRetrievalService abstractDataRetrievalService,
      List<String> encounterIdList,
      Set<Integer> orbisCodes) {

    return "kdslabor?codes="
        + commaSeparatedListOfInteger(orbisCodes)
        + "&cases="
        + String.join(DELIMITER, encounterIdList)
        + "&hideResourceTypes=ServiceRequest,DiagnosticReport";
  }

  private String commaSeparatedListOfInteger(Set<Integer> orbisCodes) {
    if (orbisCodes != null) {
      return (orbisCodes.stream().map(String::valueOf).collect(Collectors.joining(DELIMITER)));
    } else {
      return null;
    }
  }

  private static Set<Integer> getOrbisCodesAsInteger(
      Map<String, Set<Integer>> predictionModelUkbObservationOrbisCodes) {
    Set<Integer> orbisCodesAsInteger = new HashSet<>();
    predictionModelUkbObservationOrbisCodes.forEach(
        (x, y) -> {
          orbisCodesAsInteger.addAll(y);
        });
    return orbisCodesAsInteger;
  }

  @Override
  public String getUkbRenalReplacementBodyWeight(
      AcuwaveDataRetrievalService acuwaveDataRetrievalService,
      List<String> encounterIdList,
      DataSourceType dataSourceType) {
    if (dataSourceType instanceof AcuwaveDataSourceType acuwaveDataSourceType) {
      return getModuleName(acuwaveDataSourceType)
          + "?cases="
          + String.join(DELIMITER, encounterIdList)
          + "&skipDuplicateCheck=true&profileFilters=BODYWEIGHT";
    }
    return null;
  }

  @Override
  public String getUkbRenalReplacementStart(
      AcuwaveDataRetrievalService acuwaveDataRetrievalService,
      List<String> encounterIdList,
      DataSourceType dataSourceType) {

    if (dataSourceType instanceof AcuwaveDataSourceType acuwaveDataSourceType) {
      return getModuleName(acuwaveDataSourceType)
          + "?cases="
          + String.join(DELIMITER, encounterIdList)
          + "&skipDuplicateCheck=true&profileFilters=RENALREPLACEMENT";
    }
    return null;
  }

  @Override
  public String getUkbRenalReplacementUrineOutput(
      AcuwaveDataRetrievalService acuwaveDataRetrievalService,
      List<String> encounterIdList,
      DataSourceType dataSourceType) {

    if (dataSourceType instanceof AcuwaveDataSourceType acuwaveDataSourceType) {
      return getModuleName(acuwaveDataSourceType)
          + "?cases="
          + String.join(DELIMITER, encounterIdList)
          + "&skipDuplicateCheck=true&profileFilters=URINEOUTPUT&filterZeroValues=true";
    }
    return null;
  }

  private static String getModuleName(AcuwaveDataSourceType acuwaveDataSourceType) {
    String kdsModule = null;
    switch (acuwaveDataSourceType) {
      case CLAPP -> kdsModule = "kdsicu";
      case PDMS_REPORTING_DB -> kdsModule = "pdmsreporting";
    }
    return kdsModule;
  }
}
