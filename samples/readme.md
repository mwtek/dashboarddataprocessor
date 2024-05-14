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

* <a href="./SampleBundle_Covid.json" target="_blank">SampleBundle_Covid.json</a>
    * Bundle of 3 patients.
        * 3 patients with inpatient icu covid-19 cases
            * one with an in-progress encounter
            * one who got a finished case with discharge disposition 'dead' and who also got an
              alpha variant test result
            * one which an ecmo procedure that contains an additional ops code in addition to the
              snomed code
        * 1 patient with an outpatient covid-19 encounter
* <a href="./SampleBundle_Influenza.json" target="_blank">SampleBundle_Influenza.json</a>
    * Bundle of 3 patients.
        * 2 patients with inpatient influenza cases
            * 1 patient with an in-progress encounter on icu, an active ventilation and an influenza
              icd-diagnosis
            * 1 patient with a finished case with discharge disposition 'dead' who moved once from
              normal ward to icu ward, who had an ecmo procedure and a negative and who also a
              positive influenza lab result
        * 1 patient with an outpatient influenza encounter and an influenza icd-diagnosis
        * 1 female patient with a pre-stationary influenza encounter and an influenza icd-diagnosis

More bundles with sample data will surely follow in the future.

These samples will also be used for unit testing. 
