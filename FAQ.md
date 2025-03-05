# Frequently Asked Questions

- [I don't have any dischargeDisposition set up, do I have alternative options?](#dischargeDisposition)

<a name="dischargeDisposition"></a>

## I don't have any dischargeDisposition set up, do I have alternative options?

Yes, you can use `Patient.deceasedDateTime` instead. It will check whether there is an intersection
in the `Encounter.period` with any facility contact (`Einrichtungskontakt`) and if this is the case
an artificially `dischargeDisposition` = `07` ("Tod") will get added.

To activate this option you need to set the value of `use-patient-deceased` to `true` in the
`application.yaml`. 
