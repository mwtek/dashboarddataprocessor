# Release Notes - Version V0.5.4+update.10

06/May/25

## New Features

<div style='margin-left:30px;'>

* The support for the `Acribis` project just started. To activate the generation of the first two
  data items, `global.generate-acribis-data` must be set to `true` in the `application.yaml`.

</div> 

## Improvements

No improvements were part of this version.

## Tasks

<div style='margin-left:30px;'>

* Updated the data set description PDF.

</div> 

# Release Notes - Version V0.5.4+update.9

17/April/25

## New Features

No features were part of this version.

## Improvements

<div style='margin-left:30px;'>

* Added unit tests for data-items `cumulative.outpatient.gender` and `cumulative.inpatient.gender`.
* The `Observation` and `Condition` data retrieval will now use the kickoff date filter by default,
  and there is also an additional date filter in the processing logic implemented now.

</div> 

## Tasks

<div style='margin-left:30px;'>

* Renaming of the new `filter-encounter-by-date` to `filter-resources-by-date`.

</div> 

## Bugs

<div style='margin-left:30px;'>

* Since V0.5.4+update.7, the `cumulative.gender` and `cumulative.inpatient.gender` showed the same
  numbers.
* Some influenza charts erroneously included data before the kick-off date.

</div> 

# Release Notes - Version V0.5.4+update.8

10/April/25

## New Features

No features were part of this version.

## Improvements

No improvements were part of this version.

## Tasks

No tasks were part of this version.

## Bugs

<div style='margin-left:30px;'>

* The influenza encounter data retrieval uses the right cut-off-date (01.09.2022) now.

</div> 

# Release Notes - Version V0.5.4+update.7

03/April/25

## New Features

<div style='margin-left:30px;'>

* Added debug items for `cumulative.gender` and `cumulative.inpatient.gender`

</div>

## Improvements

No improvements were part of this version.

## Tasks

No tasks were part of this version.

## Bugs

No bug fixes were part of this version.

# Release Notes - Version V0.5.4+update.6

14/March/25

## New Features

<div style='margin-left:30px;'>

* Added support for all `ICU_undifferentiated` (sub)items.

</div>

## Improvements

No improvements were part of this version.

## Tasks

<div style='margin-left:30px;'>

* Procedures that are linked to a case without a single encounter that was on icu at least once are
  getting filtered now. This filter can be deactivated again via `application.yaml` configuration
  and was implemented to filter short stay op ventilation's.
* Added a new example patient to test the new behavior mentioned and updated the unit tests.

</div>

## Bugs

No bug fixes were part of this version.

# Release Notes - Version V0.5.4+update.5

05/March/25

## New Features

<div style='margin-left:30px;'>

* Added a new option to use `Patient.deceasedDateTime` to flag encounters with deceased status if
  `dischargeDisposition` is not used.

</div>

## Improvements

No improvements were part of this version.

## Tasks

<div style='margin-left:30px;'>

* Just encounters with `Encounter.status` = `in-progress` or `finished` will be used in the
  processing logic from now on, mainly to filter out canceled / uncertain cases.
* Observations and Procedures resources with status `entered-in-error` or `not-done` are getting
  ignored now as well.
* Minor refactorings and updated javadocs.

</div> 

## Bugs

No bug fixes were part of this version.

# Release Notes - Version V0.5.4+update.4

05/March/25

## New Features

<div style='margin-left:30px;'>

* Added a new option to use `Patient.deceasedDateTime` to flag encounters with deceased status if
  `dischargeDisposition` is not used.

</div>

## Improvements

No improvements were part of this version.

## Tasks

<div style='margin-left:30px;'>

* Users can now add custom code system URLs in the `application.yaml` to use local (non-SNOMED)
  identifier for qualitative lab value codes (i.e. `Observation.valueCodeableConcept.system`).
* Added a new covid pcr code to the defaults
* Added a `FAQ.md` file

</div> 

## Bugs

<div style='margin-left:30px;'>

* The `Encounter.serviceProvider` workaround to flag ICU locations does now also work in the
  influenza pipeline.

</div> 

# Release Notes - Version V0.5.4+update.3

25/February/25

## New Features

No features were part of this version.

## Improvements

No improvements were part of this version.

## Tasks

No tasks were part of this version.

## Bugs

<div style='margin-left:30px;'>

* If several `Observation.code` entries existed, the order of the codes was involuntarily decisive.

</div> 

# Release Notes - Version V0.5.4+update.2

21/February/25

## New Features

No features were part of this version.

## Improvements

No improvements were part of this version.

## Tasks

<div style='margin-left:30px;'>

* SSL-secured REST calls now include Basic Authentication if credentials are provided.

</div> 

## Bugs

No bug fixes were part of this version.

# Release Notes - Version V0.5.4+update.1

18/February/25

## New Features

No features were part of this version.

## Improvements

No improvements were part of this version.

## Tasks

<div style='margin-left:30px;'>

* If `Encounter.location.period` is missing on supply contact encounters that `Encounter.period`will
  be taken for the `*.cumulative.lengthofstay.icu` / `*.timeline.maxtreatmentlevel` calculation.

</div> 

## Bugs

<div style='margin-left:30px;'>

* Fixed an exception that appeared when the visit number of an encounter is not existing.

</div> 

# Release Notes - Version V0.5.4

13/February/25

## New Features

<div style='margin-left:30px;'>

* Full support for the new V0.5.4 kiradar data items
* Added unit tests and example data for all of the kiradar data items.
* Update of the influenza loinc codes to the [complete value set](https://loinc.org/LG32757-3)
* It is now possible to detect ICU stays via `Encounter.serviceProvider` as an alternative to
  location resource usage
* If no `Encounter.location` resources can be
  found, `Encounter.type.kontaktart` = `intensivstationaer` will be checked as alternative criteria
  to flag a supply contact encounter as icu encounter.
* Addition of an option to parameterize the qualitative codes of the laboratory results.
* It is now possible to switch the FHIR server calls where IDs are transferred from GET to POST
  requests to avoid the problem of URIs that are too long.
* Added support for token-based authentification.
* Added an option to set minimum thresholds to be reached for all data elements.

</div>

## Improvements

<div style='margin-left:30px;'>

* Adding a new file that describes the data item composition.
* Adding of many logging mechanisms for unexpected/missing fhir attributes.
* At least a reduced json output will now be generated even if the "Versorgungsstellenkontakt"
  resources cannot be used.
* Upgrade of the HAPI libraries to 7.6.0.
* Procedure retrieval on blaze servers (0.31.0+) should be optimized now.

</div> 

## Tasks

<div style='margin-left:30px;'>

* The various cutoff dates by project are now activated.
* Adding V0.5.4 data set description.
* Adding a new variant loinc code to the default list.
* Switching from `subject` to `patient` for some calls of the fhir data query, as this parameter
  does the same thing but
  gets [further support](https://hl7.org/fhir/R4/compartmentdefinition-patient.html) regarding index
  optimization.

</div> 

## Bugs

<div style='margin-left:30px;'>

* With certain FHIR server configurations, it was possible that not all Encounter resources were
  retrieved. This has now been ensured by switching to the use of the paging mechanism.
* Attributes within `Patient.address` that dont hold a real value but an (data absent) extension now
  doesn't throw NPE anymore.
* It is now possible again to use proprietary observation codes instead of LOINC codes.
* Possible discrepancies in the totals between the age charts have been fixed.

</div> 

# Release Notes - Version V0.5.3

14/May/24

## New Features

<div style='margin-left:30px;'>

* Addition of fhir sample data from influenza patients
* Adding unit test scenarios based on static test data json files.
* Ensuring compatibility
  with [KDS case profile 2024](https://simplifier.net/packages/de.medizininformatikinitiative.kerndatensatz.fall/2024.0.1).
* `Encounter.partOf` will be used to determine encounter hierarchy if the `Encounter.identifier` is
  not usable.
* Update of the dataset description to version 0.5.3.

</div>

## Improvements

<div style='margin-left:30px;'>

* The SNOMED `Procedure.code` will be preferred if available instead of the first one found.
* All `Location.type` entries are now searched for ICU information and not just the first one found
* A lot of consolidations and efficiency improvements in many data item generations.

</div> 

## Tasks

No tasks were part of this version.

## Bugs

* The outpatient data logic is now also totally interoperable with the newest kds case module
  profile.

# Release Notes - Version V0.5.2-pre

04/Apr/24

## New Features

<div style='margin-left:30px;'>

* Added support for the data set description 0.5.2 including most of the new influenza data items.

</div>

## Improvements

<div style='margin-left:30px;'>

* The `current.*` data items now also reliably deliver accurate outputs if
  the `Encounter.location` information are not set at the top level in the setup contact.
* Huge refactoring of almost all existing methods for data retrieval and data item generation.

</div> 

## Tasks

No tasks were part of this version.

## Bugs

No bug fixes were part of this version.

# Release Notes - Version V0.5.0-alpha2

02/Feb/24

## New Features

<div style='margin-left:30px;'>

* Adding an output field that marks ddp generated files.

</div>

## Improvements

<div style='margin-left:30px;'>

* Update of some Spring Boot libraries due to security vulnerabilities.
* Minor improvement of the documentation of the parameters in the yaml.

</div> 

## Tasks

<div style='margin-left:30px;'>

* Cleaning up some Git artifacts.

</div>

## Bugs

No bug fixes were part of this version.

# Release Notes - Version V0.5.0-alpha1

30/Nov/23

## New Features

<div style='margin-left:30px;'>

* Dockerization of the building stage.

</div>

## Improvements

<div style='margin-left:30px;'>

* No improvements were part of this version.

## Tasks

<div style='margin-left:30px;'>

* Ensuring support for providers that either got covid observations or conditions.
* The build scripts now have permissions for execution.

</div>

## Bugs

No bug fixes were part of this version.

# Release Notes - Version V0.5

25/Oct/23

## New Features

* Update of the dataset description to V0.5.0 and adding support for some new elements.
* Generation of the UKB prediction model items via AcuwaveLES data source endpoint is now
  possible.
* Added docker support.

## Improvements

* Improving error messages in the logging file.

## Tasks

<div style='margin-left:30px;'>

* Making the processor fully compatible with the current status of the new case module.
* Ensuring compatibility with &quot;Versorgungsfall-/Abteilungsfall-/Einrichtungsfall&quot;-Logik.

</div>

## Bugs

No bug fixes were part of this version.

# Release Notes - Version v0.3.0-alpha5

14/Dec/22

## New Features

<div style='margin-left:30px;'>

* Using `Observation.interpretation` if `Observation.value` is empty for the detection of positive
  lab
  results.

</div>

## Improvements

<div style='margin-left:30px;'>

* Immediate termination of the workflow if no observation or condition resources are found.

</div>

## Tasks

No tasks were part of this version.

## Bugs

<div style='margin-left:30px;'>

* Minimal deviations in `timeline.tests` compared to `cumulative.results` values.

</div>

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

* Inconsistencies in the counting of the `timeline.maxtreatmentlevel`.
* Retrieving `Observation.valueCodeableConcept` can throw NPE if no code with a valid system is
  found.

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

</div> </div>

