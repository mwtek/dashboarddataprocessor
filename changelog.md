# Release Notes - Version V0.3.0-alpha1

26/Jul/22

## New Features

<div style='margin-left:30px;'>

* Making the Encounter.class checks compatible with previous CDS case module versions
* Implementation of manual configuration of loinc codes for covid lab codes
* Expansion of the value set of qualitative laboratory results

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

* Adding support for the data item: timeline.varianttestresults
* Adding support for the data item: cumulative.varianttestresults
* Making the processor compatible with Acuwaveles server (instead of FHIR)

</div>

## Improvements

<div style='margin-left:30px;'>

* Upgrading to Java 17
* Expansion of pre-filtering of cases to the ICU only cases

</div>

## Tasks

<div style='margin-left:30px;'>

* Changing the encounter fhir search filtering parameter from &quot;location-period&quot; to
  &quot;date&quot;
* Removal of two outdated age items from the output
* Making the data retrieval compatible with IBM FHIR server.
* Adding/updating documentation files to the current version 0.3
* Ensuring that all data retrieval operations thread-safe
* Removing of all the newadmission.* items from logic and output
* Updating the ICD code handling to the new 'KDS Diagnose' profile
* Switching to english data item names from the 0.3.0 specification
* Code refactoring of the Dashboardlogic
* Updating the dashboard documentation to the changes in the kds case module

</div>

## Bugs

<div style='margin-left:30px;'>

* OperationOutcome resources in the bundle throw ClassCastExceptions
* Handling of icu location resources throws a NPE if a non-ward location is found
* Cumulative.gender and cumulative.results are both excluded if latter one is excluded
* NPE when patient.address is missing

</div>

