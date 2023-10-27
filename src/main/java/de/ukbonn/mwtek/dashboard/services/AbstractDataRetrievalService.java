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

import de.ukbonn.mwtek.dashboard.configuration.GlobalConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.SearchConfiguration;
import de.ukbonn.mwtek.dashboard.interfaces.DataRetrievalService;
import de.ukbonn.mwtek.dashboard.interfaces.DataSourceType;
import de.ukbonn.mwtek.dashboard.interfaces.SearchService;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;

/**
 * All methods for retrieving the data required for the Corona dashboard from any supported server.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
public abstract class AbstractDataRetrievalService implements DataRetrievalService {

  protected Set<String> patientIds = ConcurrentHashMap.newKeySet();
  protected Set<String> encounterIds = ConcurrentHashMap.newKeySet();
  @Getter
  protected Set<String> locationIds = ConcurrentHashMap.newKeySet();

  /**
   * Since the data retrieval service is independent of a dedicated server type, a corresponding
   * service that handles server queries must be passed.
   */
  @Getter
  @Setter
  private SearchService searchService;
  /**
   * The data type of the (SARS-CoV-2 PCR in this case) lab codes can be variable depending on the
   * server and varies between textual and numerical values as expected input.
   */
  @Getter
  @Setter
  private List<String> labPcrCodes;

  /**
   * A textual list of LOINC codes used to determine covid variants.
   */
  @Getter
  @Setter
  private List<String> labVariantCodes;

  /**
   * A list of covid relevant ICD diagnosis codes (usually U07.1 and U07.2).
   */
  @Getter
  @Setter
  private List<String> icdCodes;

  /**
   * A list of covid relevant snomed procedure codes that identifies artificial ventilation
   * procedures
   */
  @Getter
  @Setter
  private List<String> procedureVentilationCodes;

  /**
   * A list of covid relevant snomed procedure codes that identifies ecmo procedures
   */
  @Getter
  @Setter
  private List<String> procedureEcmoCodes;

  @Getter
  @Setter
  private int maxCountSize;

  @Getter
  @Setter
  private SearchConfiguration searchConfiguration;

  @Getter
  @Setter
  private GlobalConfiguration globalConfiguration;

  public Boolean getFilterEncounterByDate() {
    return searchConfiguration.getFilterEncounterByDate();
  }

  /**
   * TODO
   */
  @Getter
  @Setter
  private List<String> predictionModelUkbObservationCodes;

  public abstract List<CoreBaseDataItem> getUkbRenalReplacementBodyWeight(
      Collection<String> encounterIds, DataSourceType dataSourceType);

  public abstract List<CoreBaseDataItem> getUkbRenalReplacementStart(
      Collection<String> icuLocalCaseIds, DataSourceType dataSourceType);

  public abstract List<CoreBaseDataItem> getUkbRenalReplacementUrineOutput(
      Collection<String> icuLocalCaseIds, DataSourceType dataSourceType);
}
