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

import static de.ukbonn.mwtek.dashboardlogic.logic.DiseaseResultFunctionality.extractIdFromReference;
import static de.ukbonn.mwtek.utilities.fhir.mapping.kdscase.valuesets.KdsEncounterFixedValues.CASETYPE_INTENSIVESTATIONARY;
import static de.ukbonn.mwtek.utilities.fhir.misc.LocationTools.ICU_DUMMY_ID;
import static de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter.getContactType;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;

@Slf4j
public class IcuStayDetection {

  /**
   * Adds a dummy ICU location to the given encounter under specific conditions.
   *
   * <p>This method checks two criteria to decide whether to add a dummy ICU location:
   *
   * <ol>
   *   <li>If the encounter's case type indicates 'intensivstationaer' (intensive stationary care).
   *   <li>If a service provider identifier is found and matches certain ICU location IDs.
   * </ol>
   *
   * If any of the conditions are satisfied, appropriate dummy ICU location(s) will be added.
   *
   * @param encounter The {@link Encounter} object to evaluate and modify.
   * @param serviceProviderIdentifierFound A flag indicating if the service provider identifier was
   *     found.
   * @param icuLocationIdsServiceProvider A set of ICU location IDs used for validation against the
   *     service provider.
   */
  public static void addIcuDummyLocationDueOtherCriteria(
      Encounter encounter,
      boolean serviceProviderIdentifierFound,
      Set<String> icuLocationIdsServiceProvider) {
    if (!encounter.hasLocation()) {
      // First option: Check if the case type is 'intensivstationaer'
      if (isCaseTypeIntensiveStationary(encounter)) {
        addIcuDummyLocation(encounter);
        log.debug("Adding dummy ICU location for case type 'intensivstationaer'.");
        return;
      }

      // Second option: Check if service provider criteria are met and add a dummy ICU location
      if (serviceProviderIdentifierFound && encounter.hasServiceProvider()) {
        addIcuLocationByServiceProviderIdCheck(encounter, icuLocationIdsServiceProvider);
        log.debug("Adding ICU location based on service provider ID check.");
      }
    }
  }

  /**
   * A simple check if the given contact type got the value "intensivstationaer".
   *
   * @return <code>True</code> if the case type equals "intensivstationaer".
   */
  private static boolean isCaseTypeIntensiveStationary(Encounter encounter) {
    if (encounter.hasType()) {
      String contactType = getContactType(encounter.getType());
      return contactType != null && contactType.equals(CASETYPE_INTENSIVESTATIONARY);
    }
    return false;
  }

  private static void addIcuLocationByServiceProviderIdCheck(
      Encounter encounter, Set<String> icuLocationIdsServiceProvider) {
    String serviceProviderId;
    // Try to retrieve the id via reference, otherwise via identifier
    if (encounter.getServiceProvider().hasReference())
      serviceProviderId = extractIdFromReference(encounter.getServiceProvider());
    else serviceProviderId = encounter.getServiceProvider().getIdentifier().getValue();
    // Check if the service provider's ID matches any ICU location ID
    if (serviceProviderId != null && icuLocationIdsServiceProvider.contains(serviceProviderId)) {
      addIcuDummyLocation(encounter);
    }
  }

  private static void addIcuDummyLocation(Encounter encounter) {
    encounter
        .addLocation()
        .setLocation(new Reference().setReference(String.format("Location/%s", ICU_DUMMY_ID)))
        .setPeriod(encounter.getPeriod());
  }
}
