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

import de.ukbonn.mwtek.dashboard.DashboardApplication;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import java.util.Collection;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Auxiliary methods to determine if certain attributes exist for better feedback to the user. */
@Slf4j
public class ProcessHelper {

  static Logger logger = LoggerFactory.getLogger(DashboardApplication.class);

  /**
   * Could a reference to the patient be found in at least one FHIR resource (usually via the
   * "Subject" attribute)?
   *
   * @param patientIds A set of {@link UkbPatient#getPatientId() patient ids}.
   * @param resourceType The fhir resource type of the context, e.g. {@link ResourceType#Procedure}.
   * @return <code>True</code>, if patient ids could be determined.
   */
  public static boolean patientIdsCouldBeFound(Set<?> patientIds, ResourceType resourceType) {
    boolean patientIdsCouldBeFound = patientIds != null && !patientIds.isEmpty();
    // If the set is empty after the observation and condition queries have run, most of the data
    // items can no longer be filled.
    if (!patientIdsCouldBeFound) {
      logger.error(
          "No {} resources could be retrieved because no patient IDs could be determined from the"
              + " input filters (observation + condition).",
          resourceType.name());
    }
    return patientIdsCouldBeFound;
  }

  /**
   * Could a reference to the case be found in at least one FHIR resource (usually via the "Context"
   * attribute)?
   *
   * @param encounterIds A set of {@link UkbEncounter#getCaseId() case ids}.
   * @param resourceType The fhir resource type of the context, e.g. {@link
   *     ResourceType#Observation}.
   * @return <code>True</code>, if encounter ids could be determined.
   */
  public static boolean encounterIdsCouldBeFound(
      Collection<?> encounterIds, ResourceType resourceType) {
    boolean encounterIdsCouldBeFound = encounterIds != null && !encounterIds.isEmpty();
    // If the set is empty after the observation and condition queries have run, most of the data
    // items can no longer be filled.
    if (!encounterIdsCouldBeFound) {
      logger.warn(
          "No {} resources could be retrieved because no encounter IDs were found.",
          resourceType.name());
    }
    return encounterIdsCouldBeFound;
  }

  /**
   * Could a reference to the location be found in at least one FHIR encounter resource?
   *
   * @param locationIds A set of {@link UkbLocation#getId() location ids}.
   * @param resourceType The fhir resource type of the context, which will mainly be {@link
   *     ResourceType#Location} here.
   * @return <code>True</code>, if location ids could be determined.
   */
  public static boolean locationIdsCouldBeFound(Set<?> locationIds, ResourceType resourceType) {
    boolean locationIdsCouldBeFound = locationIds != null && !locationIds.isEmpty();
    // If the set is empty after the observation and condition queries have run, most of the data
    // items can no longer be filled.
    if (!locationIdsCouldBeFound) {
      logger.info(
          "No {} resources could be retrieved because no location IDs were found.",
          resourceType.name());
    }
    return locationIdsCouldBeFound;
  }
}
