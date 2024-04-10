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

package de.ukbonn.mwtek.dashboard.controller;

import static de.ukbonn.mwtek.dashboard.examples.PatientExampleData.getConditions;
import static de.ukbonn.mwtek.dashboard.examples.PatientExampleData.getEncounters;
import static de.ukbonn.mwtek.dashboard.examples.PatientExampleData.getLocations;
import static de.ukbonn.mwtek.dashboard.examples.PatientExampleData.getObservations;
import static de.ukbonn.mwtek.dashboard.examples.PatientExampleData.getPatients;
import static de.ukbonn.mwtek.dashboard.examples.PatientExampleData.getProcedures;

import de.ukbonn.mwtek.dashboardlogic.DataItemGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CoronaResultFunctionalityTests {

  @Test
  @DisplayName("Getting data items for a run with at least one example patient.")
  void testGeneratingDataItems() {
    // Initializing the data item generator with example data
    DataItemGenerator dataItemGenerator = new DataItemGenerator(getConditions(),
        getObservations(), getPatients(), getEncounters(), getProcedures(), getLocations());
// TODO update these tests
//    List<DiseaseDataItem> resultData = dataItemGenerator.getDataItems(null, false, null,
//        InputCodeSettingsExampleData.getExampleData(), DataItemContext.COVID);

  }

}
