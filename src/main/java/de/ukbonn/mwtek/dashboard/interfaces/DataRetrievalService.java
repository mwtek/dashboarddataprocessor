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
import de.ukbonn.mwtek.dashboardlogic.models.PidTimestampCohortMap;
import de.ukbonn.mwtek.dashboardlogic.predictiondata.ukb.renalreplacement.models.CoreBaseDataItem;
import de.ukbonn.mwtek.dashboardlogic.settings.QualitativeLabCodesSettings;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiConsent;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiProcedure;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiQuestionnaireResponse;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.web.client.RestClientException;

/**
 * This interface provides all the retrieval methods needed to supply the corona dashboard with the
 * necessary data, regardless of the server type.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
public interface DataRetrievalService {

  /**
   * The retrieval of FHIR {@link MiiObservation} resources.
   *
   * @return A list of all FHIR observation resources that include a covid finding.
   */
  List<Observation> getObservations(DataItemContext dataItemContext)
      throws RestClientException, OutOfMemoryError;

  /**
   * The retrieval of FHIR {@link MiiObservation} resources via encounter resources.
   *
   * @return A list of all FHIR condition resources that include a covid diagnosis.
   */
  List<MiiObservation> getObservations(
      Collection<MiiEncounter> encounters,
      DataItemContext dataItemContext,
      List<String> loincCodes,
      QualitativeLabCodesSettings qualitativeLabCodesSettings)
      throws RestClientException, OutOfMemoryError;

  /**
   * The retrieval of FHIR {@link MiiCondition} resources.
   *
   * @return A list of all FHIR condition resources that include a covid diagnosis.
   */
  List<Condition> getConditions(DataItemContext dataItemContext)
      throws RestClientException, OutOfMemoryError;

  /**
   * The retrieval of FHIR {@link MiiCondition} resources via encounter resources.
   *
   * @return A list of all FHIR condition resources that include a covid diagnosis.
   */
  List<MiiCondition> getConditions(
      Collection<MiiEncounter> encounters, DataItemContext dataItemContext)
      throws RestClientException, OutOfMemoryError;

  /**
   * The retrieval of FHIR {@link MiiPatient} resources.
   *
   * @param ukbObservations A list of all FHIR observation resources that include a covid finding.
   * @param ukbConditions A list of all FHIR condition resources that include a covid diagnosis.
   * @return A list of all patient resources that have at least one covid observation and/or covid
   *     diagnosis.
   */
  List<MiiPatient> getPatients(
      List<MiiObservation> ukbObservations,
      List<MiiCondition> ukbConditions,
      DataItemContext dataItemContext);

  /**
   * The retrieval of {@link MiiPatient} resources that are younger than the specified age at the
   * start of the reference date.
   *
   * @param maxAgeAtCutOffDate Maximum age in years at cut-off-date.
   * @return A list of all valid patient resources that fulfil the criteria.
   */
  List<MiiPatient> getPatients(Integer maxAgeAtCutOffDate, DataItemContext dataItemContext);

  /**
   * The retrieval of FHIR {@link MiiPatient} resources.
   *
   * @param ukbProcedures A list of all FHIR procedure resources that are concerned within the given
   *     context
   * @param ukbConditions A list of all FHIR condition resources.
   * @return A list of all patient resources that have at least one positive inclusion criteria.
   */
  List<MiiPatient> getPatients(List<MiiProcedure> ukbProcedures, List<MiiCondition> ukbConditions);

  /**
   * The retrieval of FHIR {@link MiiEncounter} resources.
   *
   * @return A list of all FHIR encounter resources of the detected patients after a specified
   *     cut-off date.
   */
  List<MiiEncounter> getEncounters(DataItemContext dataItemContext);

  /**
   * The retrieval of FHIR {@link MiiEncounter} resources.
   *
   * @return A list of all FHIR encounter resources of the detected patients after a specified
   *     cut-off date.
   */
  List<MiiEncounter> getEncounters(DataItemContext dataItemContext, List<MiiPatient> patients);

  /**
   * The retrieval of FHIR {@link MiiEncounter} resources.
   *
   * @return A list of all FHIR encounter resources of the detected patients after a specified
   *     cut-off date.
   */
  List<MiiEncounter> getEncounters(PidTimestampCohortMap pidTimestampMap);

  /**
   * The retrieval of FHIR {@link MiiProcedure} resources.
   *
   * @return A list of all requested FHIR procedure resources that contain artificial ventilation
   *     data for patients with at least one covid observation and/or covid diagnosis.
   */
  List<MiiProcedure> getProcedures(DataItemContext dataItemContext);

  /**
   * The retrieval of FHIR {@link MiiProcedure} resources.
   *
   * @return A list of all requested FHIR procedure resources that contain artificial ventilation
   *     data for patients with at least one covid observation and/or covid diagnosis.
   */
  List<MiiProcedure> getProcedures(
      Collection<MiiEncounter> encounters, DataItemContext dataItemContext);

  /**
   * The retrieval of FHIR {@link MiiProcedure} ventilation resources.
   *
   * @return A list of all requested FHIR procedure resources that contain artificial ventilation
   *     data.
   */
  List<MiiProcedure> getProcedures(
      Collection<MiiEncounter> encounters,
      DataItemContext dataItemContext,
      Boolean activeEncountersOnly);

  /**
   * The retrieval of FHIR {@link MiiProcedure} resources. The input resources are needed for a
   * preprocessing filtering in the acuwave sided procedure data retrieval.
   *
   * @param listMiiEncounters A list of all FHIR encounter resources of the detected patients after
   *     a specified cut-off date.
   * @param listMiiLocations A list of all FHIR location resources that are referenced in any
   *     encounter resources.
   * @param listUkbObservations A list of all FHIR observation resources that include a covid
   *     finding.
   * @param listUkbConditions A list of all FHIR condition resources that include a covid diagnosis.
   * @return A list of all requested FHIR procedure resources that contain artificial ventilation
   *     data for patients with at least one covid observation and/or covid diagnosis.
   */
  List<MiiProcedure> getProcedures(
      List<MiiEncounter> listMiiEncounters,
      List<MiiLocation> listMiiLocations,
      List<MiiObservation> listUkbObservations,
      List<MiiCondition> listUkbConditions,
      DataItemContext dataItemContext);

  /**
   * The retrieval of FHIR {@link MiiLocation} resources.
   *
   * @return A list of all requested FHIR location resources.
   */
  List<Location> getLocations();

  /**
   * The retrieval of FHIR {@link MiiConsent broad and acribis consent} resources.
   *
   * @return A list of all requested FHIR consent resources.
   */
  List<MiiConsent> getConsents(Collection<MiiEncounter> encountersOutput);

  /**
   * The retrieval of FHIR {@link MiiConsent broad consent} resources.
   *
   * @return A list of all requested FHIR consent resources.
   */
  List<MiiConsent> getConsents();

  /**
   * The retrieval of FHIR {@link MiiQuestionnaireResponse} ventilation resources.
   *
   * @return A list of all requested FHIR questionnaire response resources that are used to gather
   *     acribis follow-up information.
   */
  List<MiiQuestionnaireResponse> getQuestionnaireResponses(List<String> patients);

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

  public CapabilityStatement getStatus();
}
