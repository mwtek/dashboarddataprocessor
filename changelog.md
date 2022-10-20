# Release Notes - Version V0.3.0-alpha4

20/Oct/22

## New Features

<div style='margin-left:30px;'>

* Adding a set of example/test data.

</div>

## Improvements

No improvements were part of this version.

## Tasks

No tasks were part of this version.

## Bugs

<div style='margin-left:30px;'>

* Inconsistencies in the counting of the timeline.maxtreatmentlevel.
* Retrieving Observation.valueCodeableConcept can throw NPE if no code with a valid system is found.

</div>

# Release Notes - Version V0.3.0-alpha3

14/Oct/22

## New Features

<div style='margin-left:30px;'>

* Using of the `Encounter.diagnosis` reference to retrieve cases if `Condition.encounter` is null.

</div>

## Improvements

<div style='margin-left:30px;'>

* Consolidation of the filtering process of `current.treatmentlevel` and `current.maxtreatmentlevel`
  .
* Adding information how to extend logging / retrieve debug information to the `readme.md` file.
* Clearing of obsolete statements in the handling of the `cumulative.maxtreatmentlevel`.
* Removal of all direct index accesses.
* Optimizing of the `cumulative.maxtreatmentlevel` calculation.

</div>

## Tasks

<div style='margin-left:30px;'>

* Updating of the HAPI libraries to HAPI 6.

</div>

## Bugs

<div style='margin-left:30px;'>

* Paging does not work properly on Blaze servers without delivering an error message.
* Procedure resources without an encounter reference can evoke NPE.
* NPE in the contact type and the `Patient.address.country` retrieval.
* NPE if `Encounter.type.KontaktArt.coding.system` is null.

</div>

# Release Notes - Version V0.3.0-alpha2

23/Aug/22

## New Features

No features were part of this version.

## Improvements

No improvements were part of this version.

## Tasks

<div style='margin-left:30px;'>

* Switching procedure retrieval from `Procedure.category.coding` to `Procedure.coding`.
* Correction of the data item name for the chart of positive tests.
* Fixing NFE in the generation of the `timeline.maxtreatmentlevel`.
* Implementation of a configurable procedure code specification (OPS code support).

</div>

## Bugs

<div style='margin-left:30px;'>

* Error in counting `cumulative.age.maxtreatmentlevel.*` for very special case constellations.
* NPE if `Observation.value` is null.
* Typo in the variant gamma code.

</div>

# Release Notes - Version V0.3.0-alpha1

26/Jul/22

## New Features

<div style='margin-left:30px;'>

* Making the `Encounter.class` checks compatible with previous CDS case module versions.
* Implementation of manual configuration of loinc codes for covid lab codes.
* Expansion of the value set of qualitative laboratory results.

</div>

## Improvements

No improvements were part of this version.

## Tasks

No tasks were part of this version.

## Bugs

No bug fixes were part of this version.

# Release Notes - Version V0.3.0

30/Jun/22

## New Features

<div style='margin-left:30px;'>

* Adding support for the data item: `timeline.varianttestresults`.
* Adding support for the data item: `cumulative.varianttestresults`.
* Making the processor compatible with Acuwaveles server (instead of FHIR).

</div>

## Improvements

<div style='margin-left:30px;'>

* Upgrading to Java 17.
* Expansion of pre-filtering of cases to the ICU only cases.

</div>

## Tasks

<div style='margin-left:30px;'>

* Changing the encounter fhir search filtering parameter from &quot;location-period&quot; to
  &quot;date&quot;
* Removal of two outdated age items from the output.
* Making the data retrieval compatible with IBM FHIR server.
* Adding/updating documentation files to the current version 0.3.
* Ensuring that all data retrieval operations thread-safe.
* Removing of all the `newadmission.*` items from logic and output.
* Updating the ICD code handling to the new `KDS Diagnose` profile.
* Switching to english data item names from the 0.3.0 specification.
* Code refactoring of the `dashboardlogic`.
* Updating the dashboard documentation to the changes in the kds case module.

</div>

## Bugs

<div style='margin-left:30px;'>

* `OperationOutcome` resources in the bundle throw `ClassCastException`s.
* Handling of icu location resources throws a NPE if a non-ward location is found.
* Cumulative.gender and `cumulative.results` are both excluded if latter one is excluded.
* NPE when `patient.address` is missing.

</div>

