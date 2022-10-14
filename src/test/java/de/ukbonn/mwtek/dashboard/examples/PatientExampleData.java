/*
 *  Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 *  modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 *  PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 *  OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 *  IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 *  YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 *  OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 *  COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 *  BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 *  OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 *  PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 *  OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with
 *  this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */

package de.ukbonn.mwtek.dashboard.examples;

import de.ukbonn.mwtek.dashboardlogic.enums.CoronaFixedValues;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbCondition;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbEncounter;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbLocation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbObservation;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbPatient;
import de.ukbonn.mwtek.utilities.fhir.resources.UkbProcedure;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Procedure.ProcedureStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.codesystems.LocationPhysicalType;
import org.hl7.fhir.r4.model.codesystems.ObservationStatus;

public class PatientExampleData {

  public static String TOPLEVEL_RESOURCE_ID_P1 = "1234";
  public static String TOPLEVEL_RESOURCE_ID_P2 = "1235";

  public static List<UkbObservation> getObservations() {

    List<UkbObservation> exampleResources = new ArrayList<>();

    CodeableConcept codeableConceptCovidObs = new CodeableConcept();
    codeableConceptCovidObs.addCoding(
        new Coding(CoronaFixedValues.LOINC_SYSTEM, "94306-8", ""));
    UkbObservation exampleResource = new UkbObservation(TOPLEVEL_RESOURCE_ID_P1,
        TOPLEVEL_RESOURCE_ID_P1,
        new Observation.ObservationStatusEnumFactory().fromType(
            new StringType(ObservationStatus.FINAL.toCode())),
        codeableConceptCovidObs);

    exampleResource.setId(TOPLEVEL_RESOURCE_ID_P1);

    // Setting of a qualitative value of the observation
    exampleResource.setValue(
        new CodeableConcept().addCoding(new Coding("http://snomed.info", "10828004", "Positive")));

    exampleResources.add(exampleResource);

    return exampleResources;
  }

  public static List<UkbCondition> getConditions() {

    List<UkbCondition> exampleResources = new ArrayList<>();

    UkbCondition exampleResource = new UkbCondition(TOPLEVEL_RESOURCE_ID_P1,
        TOPLEVEL_RESOURCE_ID_P1,
        new CodeableConcept().addCoding(
            new Coding("http://terminology.hl7.org/CodeSystem/condition-clinical", "active",
                "active")), new CodeableConcept().addCoding(
        new Coding(CoronaFixedValues.ICD_SYSTEM.getValue(), "U07.1", "COVID-19")),
        new DateType(new Date()).getValue());

    exampleResource.setId(TOPLEVEL_RESOURCE_ID_P1);
    exampleResources.add(exampleResource);

    return exampleResources;
  }

  public static List<UkbEncounter> getEncounters() {

    List<UkbEncounter> exampleResources = new ArrayList<>();

    UkbEncounter exampleResource = new UkbEncounter(TOPLEVEL_RESOURCE_ID_P1,
        new Encounter.EncounterStatusEnumFactory().fromType(
            new StringType(EncounterStatus.INPROGRESS.toCode())),
        new Coding("http://fhir.de/ValueSet/EncounterClassDE", "IMP", null));
    exampleResource.addLocation(
        new EncounterLocationComponent(
            new Reference("Location/"
                + TOPLEVEL_RESOURCE_ID_P1)));
    exampleResource.setId(TOPLEVEL_RESOURCE_ID_P1);
    exampleResource.setPeriod(new Period().setStart(new Date()));
    exampleResources.add(exampleResource);

    // Missing code system in codings should all be handled and not throw any exception.
    UkbEncounter encounterWithMissingCodeSystems = new UkbEncounter(TOPLEVEL_RESOURCE_ID_P2,
        new Encounter.EncounterStatusEnumFactory().fromType(
            new StringType(EncounterStatus.INPROGRESS.toCode())),
        new Coding(null, "IMP", null));
    // Usually the encounter.type.kontaktart.system = "http://fhir.de/CodeSystem/kontaktart-de"
    encounterWithMissingCodeSystems.addType(
        new CodeableConcept().addCoding(new Coding(null, "vorstationaer", "vorstationaer")));

    exampleResources.add(encounterWithMissingCodeSystems);

    return exampleResources;
  }

  public static List<UkbPatient> getPatients() {

    List<UkbPatient> exampleResources = new ArrayList<>();

    List<Identifier> identifiers = new ArrayList<>();
    identifiers.add(
        new Identifier().setValue(TOPLEVEL_RESOURCE_ID_P1).setUse(IdentifierUse.OFFICIAL));
    List<HumanName> humanNames = new ArrayList<>();
    humanNames.add(new HumanName().setFamily("Testpatient"));
    List<Address> addresses = new ArrayList<>();
    addresses.add(new Address().setPostalCode("12345"));
    UkbPatient exampleResource = new UkbPatient(identifiers, humanNames,
        AdministrativeGender.MALE, addresses);

    exampleResource.setId(TOPLEVEL_RESOURCE_ID_P1);
    exampleResources.add(exampleResource);

    return exampleResources;
  }

  public static List<UkbProcedure> getProcedures() {

    List<UkbProcedure> exampleResources = new ArrayList<>();

    UkbProcedure exampleResource = new UkbProcedure(TOPLEVEL_RESOURCE_ID_P1,
        TOPLEVEL_RESOURCE_ID_P1,
        ProcedureStatus.INPROGRESS, new CodeableConcept().addCoding(
        new Coding("http://snomed.info/sct", "243147009", "Controlled ventilation (procedure)")),
        new Period().setStart(new Date()));

    exampleResource.setId(TOPLEVEL_RESOURCE_ID_P1);
    exampleResources.add(exampleResource);

    return exampleResources;
  }

  public static List<UkbLocation> getLocations() {

    List<UkbLocation> exampleResources = new ArrayList<>();
    List<Identifier> identifiers = new ArrayList<>();
    identifiers.add(new Identifier().setValue(TOPLEVEL_RESOURCE_ID_P1)
        .setUse(IdentifierUse.OFFICIAL));
    UkbLocation exampleResource = new UkbLocation(identifiers,
        new CodeableConcept().addCoding(
            new Coding("http://terminology.hl7.org/CodeSystem/location-physical-type",
                LocationPhysicalType.WA.toCode(), LocationPhysicalType.WA.getDisplay())));
// Marking it as icu unit
    List<CodeableConcept> types = new ArrayList<>();
    types.add(new CodeableConcept().addCoding(
        new Coding("http://terminology.hl7.org/CodeSystem/v3-RoleCode", "ICU",
            "Intensive care unit")));

    exampleResource.setType(types);

    exampleResource.setId(TOPLEVEL_RESOURCE_ID_P1);
    exampleResources.add(exampleResource);

    return exampleResources;
  }

}
