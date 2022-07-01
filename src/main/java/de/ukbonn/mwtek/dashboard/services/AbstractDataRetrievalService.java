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

import de.ukbonn.mwtek.dashboard.configuration.SearchConfiguration;
import de.ukbonn.mwtek.dashboard.interfaces.DataRetrievalService;
import de.ukbonn.mwtek.dashboard.interfaces.SearchService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * All methods for retrieving the data required for the Corona dashboard from any supported server.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
public abstract class AbstractDataRetrievalService implements DataRetrievalService {

  protected Set<String> patientIds = ConcurrentHashMap.newKeySet();
  protected Set<String> encounterIds = ConcurrentHashMap.newKeySet();
  protected Set<String> locationIds = ConcurrentHashMap.newKeySet();

  /**
   * Since the data retrieval service is independent of a dedicated server type, a corresponding
   * service that handles server queries must be passed.
   */
  private SearchService searchService;
  /**
   * The data type of the (SARS-CoV-2 PCR in this case) lab codes can be variable depending on the
   * server and varies between textual and numerical values as expected input.
   */
  private List<?> labCodes;
  /**
   * A list of Covid relevant ICD diagnosis codes (usually U07.1 and U07.2).
   */
  private List<String> icdCodes;

  /**
   * A list of Covid relevant OPS procedure codes that identifies artificial ventilation procedures
   */
  private List<String> opsCodes;

  private int maxCountSize;

  private SearchConfiguration searchConfiguration;

  void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  public SearchService getSearchService() {
    return searchService;
  }

  void setLabCodes(List<?> labCodes) {
    this.labCodes = labCodes;
  }

  public List<?> getLabCodes() {
    return labCodes;
  }

  void setIcdCodes(List<String> icdCodes) {
    this.icdCodes = icdCodes;
  }

  public List<String> getIcdCodes() {
    return icdCodes;
  }

  void setOpsCodes(List<String> opsCodes) {
    this.opsCodes = opsCodes;
  }

  public List<String> getOpsCodes() {
    return opsCodes;
  }

  public Boolean getFilterEncounterByDate() {
    return searchConfiguration.getFilterEncounterByDate();
  }

  public void setMaxCountSize(int maxCountSize) {
    this.maxCountSize = maxCountSize;
  }

  public int getMaxCountSize() {
    return maxCountSize;
  }

  public void setSearchConfiguration(SearchConfiguration searchConfiguration) {
    this.searchConfiguration = searchConfiguration;
  }

  public SearchConfiguration getSearchConfiguration(SearchConfiguration searchConfiguration) {
    return searchConfiguration;
  }

}
