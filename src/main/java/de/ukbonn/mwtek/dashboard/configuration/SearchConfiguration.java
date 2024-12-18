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

package de.ukbonn.mwtek.dashboard.configuration;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

/**
 * This class is used to parameterize the Search queries at the local FHIR/ACUWAVE server of the
 * data providers.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Getter
@Setter
@Configuration
public abstract class SearchConfiguration {

  /**
   * Filter the patient (and thus also the encounter) retrieval to disease-positive-patients only
   */
  private Boolean filterPatientRetrieval = true;

  /**
   * Filter the procedure retrieval to inpatient disease-positive-cases only (with at least one icu
   * transfer) to reduce the requests on an external server
   */
  private Boolean filterProcedureRetrieval = true;

  /**
   * Should procedures also be queried for specific non-ICU wards? If yes, specify the orbis
   * internal OEBENEID here.
   */
  private List<String> filterProcedureRetrievalAdditionalWards = new ArrayList<>();

  /**
   * Should the Encounter FHIR search query be filtered by admission date (All cases by
   * disease-positive-encounter start reference date)? Must be disabled if outpatient cases do not
   * have an end date.
   */
  private Boolean filterEncounterByDate = true;

  /** batch size of the parallelized partial searches */
  private int batchSize = 500;

  /**
   * Filter the patient (and thus also the encounter) retrieval to influenza positive patients only
   * which will reduce the data load
   */
  private Boolean influenzaFilterPatientRetrieval = true;
}
