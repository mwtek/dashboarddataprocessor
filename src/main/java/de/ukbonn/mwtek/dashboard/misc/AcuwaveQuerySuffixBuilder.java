package de.ukbonn.mwtek.dashboard.misc;/*
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

import de.ukbonn.mwtek.dashboard.interfaces.QuerySuffixBuilder;
import de.ukbonn.mwtek.dashboard.services.AbstractDataRetrievalService;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Building the templates of the individual REST requests to the Acuwaveles server.
 */
public class AcuwaveQuerySuffixBuilder implements QuerySuffixBuilder {

  public String getObservations(AbstractDataRetrievalService acuwaveDataRetrievalService,
      Integer month) {
    return "kdslabor?codes=" + acuwaveDataRetrievalService.getLabCodes().stream()
        .map(String::valueOf).collect(Collectors.joining(
            ",")) + "&months=" + month + "&hideResourceTypes=ServiceRequest,DiagnosticReport";
  }

  public String getConditions(AbstractDataRetrievalService acuwaveDataRetrievalService,
      Integer month) {
    return "kdsdiagnose?codes=" + String.join(",",
        acuwaveDataRetrievalService.getIcdCodes()) + "&months=" + month;
  }

  @Override
  public String getPatients(AbstractDataRetrievalService abstractRestConfiguration,
      List<String> patientIdList) {
    return "kdsperson?patients=" + String.join(",",
        patientIdList) + "&hideResourceTypes=Observation";
  }

  @Override
  public String getEncounters(AbstractDataRetrievalService abstractRestConfiguration,
      List<String> patientIdList) {
    return "kdsfall?patients=" + String.join(",", patientIdList) + "&admissionDateFrom=2020-03-01";
  }

  @Override
  public String getProcedures(AbstractDataRetrievalService abstractRestConfiguration,
      List<String> encounterIdList) {
    return "kdsicu?cases=" + String.join(",", encounterIdList) + "&skipDuplicateCheck=true";
  }

  @Override
  public String getLocations(AbstractDataRetrievalService abstractRestConfiguration,
      List<?> locationIdSublist) {
    return "location?ids=" + locationIdSublist.stream().map(String::valueOf)
        .collect(Collectors.joining(","));
  }
}
