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

import java.util.List;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;

/**
 * 
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 *
 */
public class ResourceHandler {

  /**
   * Reads the Fhir bundle, adds all entries of the Observation type to a list and adds the
   * {@link Patient patient} ids and {@link Encounter encounter} ids to a given set.
   * 
   * @param bundleResponse A FHIR response bundle that contains {@link Observation} resources
   * @param listObservations List with FHIR-Observations in which the entries from the bundle are to
   *          be stored
   * @param patientIds List of ids of the {@link Patient} resource to be extended by entries from
   *          the Observation resource.
   * @param encounterIds List of ids of the {@link Encounter} resource to be extended by entries
   *          from the Observation resource.
   */
  public static void handleObservationEntries(Bundle bundleResponse,
      List<Observation> listObservations, Set<String> patientIds, Set<String> encounterIds) {
    bundleResponse.getEntry().forEach(entry -> {
      if (entry.getResource().getClass() == Observation.class) {
        Observation obs = (Observation) entry.getResource();
        patientIds.add(obs.getSubject().getReference().split("/")[1]);
        encounterIds.add(obs.getSubject().getReference().split("/")[1]);
        listObservations.add(obs);
      }
    });
  }


  /**
   * Reads the Fhir bundle, adds all entries of the Condition type to a list and adds the
   * {@link Patient patient} ids and {@link Encounter encounter} ids to a given set.
   * 
   * @param initialBundle A FHIR response bundle that contains {@link Observation} resources
   * @param listConditions List with FHIR-{@link Condition conditions} in which the entries from the
   *          bundle are to be stored
   * @param patientIds List of ids of the {@link Patient} resource to be extended by entries from
   *          the Observation resource.
   * @param encounterIds List of ids of the {@link Encounter} resource to be extended by entries
   *          from the Observation resource.
   */
  public static void handleConditionEntries(Bundle initialBundle, List<Condition> listConditions,
      Set<String> patientIds, Set<String> encounterIds) {
    initialBundle.getEntry().forEach(entry -> {
      if (entry.getResource().getClass() == Condition.class) {
        Condition cond = (Condition) entry.getResource();
        patientIds.add(cond.getSubject().getReference().split("/")[1]);
        encounterIds.add(cond.getSubject().getReference().split("/")[1]);
        listConditions.add(cond);
      }
    });
  }

}
