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
    * Bundle of 4 patients.
        * 3 patients with inpatient icu covid-19 cases
            * one with an in-progress encounter
            * one who got a finished case with discharge disposition 'dead' and who also got an
              alpha variant test result
            * one which an ecmo procedure that contains an additional ops code in addition to the
              snomed code
        * 1 patient with an outpatient covid-19 encounter
* <a href="./SampleBundle_Influenza.json" target="_blank">SampleBundle_Influenza.json</a>
    * Bundle of 4 patients.
        * 2 patients with inpatient influenza cases
            * 1 patient with an in-progress encounter on icu, an active ventilation and an influenza
              icd-diagnosis
            * 1 patient with a finished case with discharge disposition 'dead' who moved once from
              normal ward to icu ward, who had an ecmo procedure and a negative and who also a
              positive influenza lab result
        * 1 patient with an outpatient influenza encounter and an influenza icd-diagnosis
        * 1 female patient with a pre-stationary influenza encounter and an influenza icd-diagnosis
* <a href="./SampleBundle_KidsRadar.json" target="_blank">SampleBundle_KidsRadar.json</a>
    * Bundle of 4 patients.
        * A patient with 5 cases, of which the second and the third case qualify for the case
          merging logic and getting merged with the first due to the short time intervals between
          the
          cases
            * The patient is 17 years old at the start of the first case, 18 years old at the start
              of the second case
            * The first case has a F63.8 diagnosis ('other-psychological-disorders'),
            * The second case has a F94.1 diagnosis ('stress-related-disorders')
            * The 3rd case has a F94.1 diagnosis and the admission date is <21 days after the
              discharge date of the 2nd case, so the case merging logic takes part here as well
            * The 4th case has a F63.8 diagnosis and starts >21 days after the discharge of the 3rd
              case
            * The 5th case has a F42.0 diagnosis ('obsessive-compulsive-disorder') and is also not
              valid for further logic
        * One patient with 2 different diagnoses (F62.88, F63.1) within one diagnosis group ('
          other-psychological-disorders')
        * A patient with a J21.0 (acute-rsv-bronchiolitis) and J12.1 (rsv-pneumonia) condition
        * A patient who got a rsv diagnosis (J21.0) and also a KJP diagnosis (F94.1).

More bundles with sample data will surely follow in the future.

These samples will also be used for unit testing. 
