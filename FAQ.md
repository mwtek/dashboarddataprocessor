# Frequently Asked Questions

- [We don't have any dischargeDisposition set up, do we have alternative options?](#we-dont-have-any-dischargedisposition-set-up-do-we-have-alternative-options)
- [We don't have any ventilation and ecmo data in our FHIR server, what can I do?](#we-dont-have-any-ventilation-and-ecmo-data-in-our-fhir-server-what-can-i-do)

## We don't have any dischargeDisposition set up, do we have alternative options?

Yes, you can use `Patient.deceasedDateTime` instead. It will check whether there is an intersection
in the `Encounter.period` with any facility contact (`Einrichtungskontakt`) and if this is the case
an artificially `dischargeDisposition` = `07` ("Tod") will get added.

To activate this option, you need to set the value of `use-patient-deceased` to `true` in the
`application.yaml`.

## We don't have any ventilation and ecmo data in our FHIR server, what can I do?

Yes, you can activate `global.use-icu-undifferentiated` in the settings and then all
maxtreatmentlevels contain the `ICU_undifferentiated` subitem which combines the `ICU` and
`ICU_with_ventilation` and `ICU_with_ecmo`. In addition, it is then possible to generate an explicit
age chart such as `cumulative.age.maxtreatmentlevel.icu_undifferentiated`, but the corresponding
items must be taken from the excludes in `excludes-data-items`. Logically, a combination of both
ways is not possible.
