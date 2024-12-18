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

package de.ukbonn.mwtek.dashboard.services;

import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.extractInputCodeSettings;
import static de.ukbonn.mwtek.dashboard.misc.ConfigurationTransformer.extractQualitativeLabCodesSettings;
import static de.ukbonn.mwtek.dashboardlogic.tools.KidsRadarTools.getIcdCodesAsString;

import de.ukbonn.mwtek.dashboard.configuration.GlobalConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.SearchConfiguration;
import de.ukbonn.mwtek.dashboard.interfaces.DataRetrievalService;
import de.ukbonn.mwtek.dashboard.interfaces.DataSourceType;
import de.ukbonn.mwtek.dashboard.interfaces.SearchService;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.logic.CoronaResultFunctionality;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * All methods for retrieving the data required for the Corona dashboard from any supported server.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
public abstract class AbstractDataRetrievalService implements DataRetrievalService {

  protected Set<String> patientIds = ConcurrentHashMap.newKeySet();
  protected Set<String> encounterIds = ConcurrentHashMap.newKeySet();
  @Getter protected Set<String> locationIds = ConcurrentHashMap.newKeySet();

  /**
   * Since the data retrieval service is independent of a dedicated server type, a corresponding
   * service that handles server queries must be passed.
   */
  @Getter @Setter private SearchService searchService;

  /**
   * The data type of the (SARS-CoV-2 PCR in this case) lab codes can be variable depending on the
   * server and varies between textual and numerical values as expected input.
   */
  @Getter @Setter private List<String> covidLabPcrCodes;

  /** A textual list of LOINC codes used to determine covid variants. */
  @Getter @Setter private List<String> covidLabVariantCodes;

  /** A list of covid-19 relevant ICD diagnosis codes (usually U07.1). */
  @Getter @Setter private List<String> covidIcdCodes;

  /**
   * A list of covid-19 relevant snomed procedure codes that identifies artificial ventilation
   * procedures
   */
  @Getter @Setter private List<String> procedureVentilationCodes;

  /** A list of covid-19 relevant snomed procedure codes that identifies ecmo procedures */
  @Getter @Setter private List<String> procedureEcmoCodes;

  /** A list of influenza relevant ICD diagnosis codes (e.g. J10.0). */
  @Getter @Setter private List<String> influenzaIcdCodes;

  /**
   * The data type of the (Influenza virus A and B RNA) lab codes can be variable depending on the
   * server and varies between textual and numerical values as expected input.
   */
  @Getter @Setter private List<String> influenzaLabPcrCodes;

  /** The icd codes for kids radar in one list. */
  public List<String> getKidsRadarIcdCodesAll() {
    List<String> allCodes = new ArrayList<>();
    allCodes.addAll(getIcdCodesAsString(kidsRadarRsvIcdCodes));
    allCodes.addAll(getIcdCodesAsString(kidsRadarKjpIcdCodes));
    return allCodes;
  }

  /** The icd codes for kids radar, split by group. */
  @Getter @Setter private Map<String, List<String>> kidsRadarKjpIcdCodes;

  /** The icd codes for kids radar, split by group. */
  @Getter @Setter private Map<String, List<String>> kidsRadarRsvIcdCodes;

  @Getter @Setter private SearchConfiguration searchConfiguration;

  @Getter @Setter private GlobalConfiguration globalConfiguration;

  public Boolean getFilterEncounterByDate() {
    return searchConfiguration.getFilterEncounterByDate();
  }

  /** The loinc codes that are used in the ukb prediction model calculation. */
  @Getter @Setter private List<String> predictionModelUkbObservationCodes;

  public abstract List<CoreBaseDataItem> getUkbRenalReplacementBodyWeight(
      Collection<String> encounterIds, DataSourceType dataSourceType);

  public abstract List<CoreBaseDataItem> getUkbRenalReplacementStart(
      Collection<String> icuLocalCaseIds, DataSourceType dataSourceType);

  public abstract List<CoreBaseDataItem> getUkbRenalReplacementUrineOutput(
      Collection<String> icuLocalCaseIds, DataSourceType dataSourceType);

  /** If a patient filter is activated, use the corresponding one. */
  protected Set<String> handleFilterPatientRetrieval(
      DataItemContext dataItemContext,
      Boolean filterEnabled,
      List<UkbObservation> ukbObservations,
      List<UkbCondition> ukbConditions) {
    if (filterEnabled) {
      Set<String> patientIdsPositive =
          CoronaResultFunctionality.getPidsPosFinding(
              ukbObservations,
              ukbConditions,
              extractInputCodeSettings(this),
              dataItemContext,
              extractQualitativeLabCodesSettings(this));
      if (!patientIdsPositive.isEmpty()) {
        return patientIdsPositive;
      } else {
        log.warn(
            "FilterPatientRetrieval is enabled, but cannot find {} positive patients. As a result, "
                + "the global unfiltered patient ID list is used.",
            dataItemContext);
      }
    }
    return patientIds;
  }
}
