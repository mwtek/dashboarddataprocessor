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

import de.ukbonn.mwtek.dashboard.enums.ServerTypeEnum;
import de.ukbonn.mwtek.dashboard.exceptions.SearchException;
import de.ukbonn.mwtek.dashboardlogic.enums.DataItemContext;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.springframework.web.client.RestClientException;

/**
 * This interface provides all the retrieval methods needed to supply the corona dashboard with the
 * necessary data, regardless of the server type.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
public interface DataRetrievalService {

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation}
   * resources.
   *
   * @return A list of all FHIR observation resources that include a covid finding.
   */
  List<Observation> getObservations(DataItemContext dataItemContext)
      throws RestClientException, OutOfMemoryError;

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition} resources.
   *
   * @return A list of all FHIR condition resources that include a covid diagnosis.
   */
  List<Condition> getConditions(DataItemContext dataItemContext)
      throws RestClientException, OutOfMemoryError;

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient} resources.
   *
   * @param listUkbObservations A list of all FHIR observation resources that include a covid
   *     finding.
   * @param listUkbConditions A list of all FHIR condition resources that include a covid diagnosis.
   * @return A list of all patient resources that have at least one covid observation and/or covid
   *     diagnosis.
   */
  List<Patient> getPatients(
      List<UkbObservation> listUkbObservations,
      List<UkbCondition> listUkbConditions,
      DataItemContext dataItemContext);

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter} resources.
   *
   * @return A list of all FHIR encounter resources of the detected patients after a specified
   *     cut-off date.
   */
  List<Encounter> getEncounters(DataItemContext dataItemContext);

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure} resources.
   *
   * @return A list of all requested FHIR procedure resources that contain artificial ventilation
   *     data for patients with at least one covid observation and/or covid diagnosis.
   */
  List<Procedure> getProcedures();

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure} resources.
   * The input resources are needed for a preprocessing filtering in the acuwave sided procedure
   * data retrieval.
   *
   * @param listUkbEncounters A list of all FHIR encounter resources of the detected patients after
   *     a specified cut-off date.
   * @param listUkbLocations A list of all FHIR location resources that are referenced in any
   *     encounter resources.
   * @param listUkbObservations A list of all FHIR observation resources that include a covid
   *     finding.
   * @param listUkbConditions A list of all FHIR condition resources that include a covid diagnosis.
   * @return A list of all requested FHIR procedure resources that contain artificial ventilation
   *     data for patients with at least one covid observation and/or covid diagnosis.
   */
  List<Procedure> getProcedures(
      List<UkbEncounter> listUkbEncounters,
      List<UkbLocation> listUkbLocations,
      List<UkbObservation> listUkbObservations,
      List<UkbCondition> listUkbConditions,
      DataItemContext dataItemContext);

  /**
   * The retrieval of FHIR {@link de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation} resources.
   *
   * @return A list of all requested FHIR location resources.
   */
  List<Location> getLocations();

  /**
   * Retrieval of the used {@link ServerTypeEnum server type}.
   *
   * @return The used {@link ServerTypeEnum server type} of the data retrieval service (e.g. {@link
   *     ServerTypeEnum#FHIR})
   */
  ServerTypeEnum getServerType();

  /**
   * Retrieval of the used batch size defined in the {@link
   * de.ukbonn.mwtek.dashboard.configuration.FhirSearchConfiguration} class.
   *
   * @return The configured batch size of the parallelized partial FHIR searches
   */
  int getBatchSize();

  List<Encounter> getIcuEncounters() throws SearchException;

  List<CoreBaseDataItem> getIcuEpisodes(Collection<String> caseIds) throws SearchException;

  List<CoreBaseDataItem> getUkbRenalReplacementObservations(
      Collection<String> encounterIds, Set<Integer> codes);
}
