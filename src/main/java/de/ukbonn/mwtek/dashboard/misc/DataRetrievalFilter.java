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
 */ package de.ukbonn.mwtek.dashboard.misc;

import de.ukbonn.mwtek.dashboardlogic.enums.EncounterReferenceTargetType;
import de.ukbonn.mwtek.utilities.fhir.resources.MiiEncounter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataRetrievalFilter {

  /**
   * Filters encounters by the configured encounter reference target type.
   *
   * @param encounters the encounters to filter
   * @param targetType the configured target encounter type
   * @return encounters matching the configured target type
   */
  public static List<MiiEncounter> filterEncountersByTargetType(
      Collection<MiiEncounter> encounters, EncounterReferenceTargetType targetType) {

    return encounters.stream().filter(targetType::matches).toList();
  }

  /**
   * Filters encounters by multiple configured encounter reference target types.
   *
   * @param encounters the encounters to filter
   * @param targetTypes the configured target encounter types
   * @return encounters matching at least one configured target type
   */
  public static List<MiiEncounter> filterEncountersByTargetTypes(
      List<MiiEncounter> encounters, Collection<EncounterReferenceTargetType> targetTypes) {

    Set<EncounterReferenceTargetType> distinctTargetTypes = new HashSet<>(targetTypes);

    return encounters.stream()
        .filter(
            encounter ->
                distinctTargetTypes.stream().anyMatch(targetType -> targetType.matches(encounter)))
        .toList();
  }
}
