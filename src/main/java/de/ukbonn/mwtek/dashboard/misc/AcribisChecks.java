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

import de.ukbonn.mwtek.dashboardlogic.enums.AcribisCohortIcdCodes;
import de.ukbonn.mwtek.dashboardlogic.enums.AcribisCohortOpsCodes;
import de.ukbonn.mwtek.dashboardlogic.models.PidTimestampCohortMap;
import de.ukbonn.mwtek.utilities.fhir.misc.FhirConditionTools;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbConsent;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AcribisChecks {

  public static boolean isConditionNeededForAcribis(UkbCondition cond) {
    return FhirConditionTools.isIcdCodeInConditionWithPrefixWildcardCheck(
        cond, AcribisCohortIcdCodes.ALL_CODES);
  }

  public static boolean isProcedureNeededForAcribis(UkbProcedure procedure) {
    return FhirConditionTools.isOpsCodeInProcedureWithPrefixWildcardCheck(
        procedure, AcribisCohortOpsCodes.ALL_CODES);
  }

  /**
   * Calculates the earliest valid Acribis permit start date for each patient ID from the given list
   * of consent records.
   *
   * <p>Currently, the earliest date is used without checking for an upper time bound. This is
   * acceptable as of now, but might need adjustment once date range restrictions are introduced.
   *
   * @param ukbConsents the list of consent records to evaluate
   * @return a map of patient IDs to their earliest valid Acribis permit start date
   */
  public static PidTimestampCohortMap calculateValidTimestampsByPid(List<UkbConsent> ukbConsents) {
    Map<String, Date> tempMap =
        ukbConsents.stream()
            .filter(UkbConsent::isAcribisConsentAllowed)
            .filter(c -> c.getPatientId() != null && c.getAcribisPermitStartDate() != null)
            .collect(
                Collectors.toMap(
                    UkbConsent::getPatientId,
                    UkbConsent::getAcribisPermitStartDate,
                    (d1, d2) -> d1.before(d2) ? d1 : d2 // Select the earliest timestamp
                    ));

    PidTimestampCohortMap result = new PidTimestampCohortMap();
    result.putAll(tempMap);
    return result;
  }
}
