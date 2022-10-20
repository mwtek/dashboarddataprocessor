This folder contains sample FHIR bundles that are intended to provide an overview of the structure
of the KDS FHIR resources that the dashboard data processor expects and processes, and also provides
a way to immediately fill a local FHIR server with sample data and test the processor with that data
set.
The sample data is intended to provide an overview and basis for discussion of the expected
structure of all FHIR resources considered in the workflow.

**All bundles are ready to be imported into any FHIR server!**

Before providing the sample data, initial imports of the complete bundles are always tested on HAPI
and Blaze server.
Feedback regarding import in FHIR servers of other types (e.g. IBM FHIR) is very welcome.

Currently, the following sample bundles exist with the following content characteristics:

* <a href="./SampleBundle_1.json" target="_blank">SampleBundle_1.json</a>
    * Bundle of 3 patients.
        * 2 patients with inpatient icu covid cases (one patient with an in-progress encounter while
          the other patients got a finished case with discharge disposition 'dead')
        * 1 patient with an outpatient covid encounter.

More bundles with sample data will surely follow in the future.

----- 

Addendum:

There is a particularity regarding the referencing of the location resources within the encounter
resources:
The facility contact ('Einrichtungskontakt') resources contain all location entries. Ideally, this
redundancy is omitted and the location data is exclusively part at granular level in the supply
level ('Versorgungsstellenkontakt') and department ('Abteilungskontakt') contacts. However, this
requires a sharp definition in the implementation
guide, <a href="https://simplifier.net/medizininformatikinitiative-modulfall/~issues/2333">which is
awaiting implementation/explanation</a>. Until clarification, the DDP will only check the Encounter
resource referenced by Observation or Condition, which is usually the facility contact. 
