# MII-FHIR-to-NUM-Dashboard-Processor

The _DashboardDataProcessor_ (DDP) provides a middleware solution which allows to transmit
aggregated, anonymous datasets to the [NUM CODEX Dashboard](http://coronadashboard.ukbonn.de) for
visualisation. The DDP was developed as part
of [NUM CODEX project](https://www.netzwerk-universitaetsmedizin.de/projekte/codex). It is an
interoperable,
fully [MII national core dataset](https://simplifier.net/organization/koordinationsstellemii/~projects)
based [FHIR](https://hl7.org/FHIR/) representation of operational patient data from the hospital
information systems as specified by
the [Medical Informatics Initiative (MII)](https://www.medizininformatik-initiative.de/en/start).
The _DashboardDataProcessor_ is currently developed further and maintained in the NUM RDP project.
The FHIR data can be provided via any standard compliant FHIR server that supports the required (
simple) features of FHIR search. This should enable new sites to contribute to the dashboard with
minimal additive effort if interoperable FHIR representations of operational data are already
available.
The development was supported by the German Federal Ministry of Science (
Bundesministerium für Bildung und Forschung, BMBF) in the context of
the [NUM RDP](https://www.netzwerk-universitaetsmedizin.de/projekte/num-rdp) (FKZ
01KX2121), NUM CODEX (FKZ 01KX2121) and MII ADMIRE and SMITH (FKZ 01ZZ1602C, FKZ 01ZZ1803Q)
projects and is further supported
by [NUM RDP](https://www.netzwerk-universitaetsmedizin.de/projekte/num-rdp) (FKZ
01KX2121).

For information regarding active participation in the dashboard endeavor, please contact us at
ddp-support@ukbonn.de. For bug reports, improvement suggestions, and related technical
conversations, please use the usual GitHub mechanisms.

MII core dataset models are maintained
on [ArtDecor](https://art-decor.org/art-decor/decor-project--mide-). The current MII core dataset
specifications are available
on [Simplifier](https://simplifier.net/organization/koordinationsstellemii/~projects).

## Structure Overview

The following table gives an overview of the documents used, as well as their corresponding links
and a short description.

<table style="width:100%" >
<tr>
<th>Document name and link</th>
<th>Description</th>
</tr>
<tr>
<td><a href="./files/Datensatzbeschreibung_Dashboard_v0_5_4_20241028.pdf" target="_blank">JSON dataset description</a></td>
<td>The dataset description of the resulting JSON format, including the description of the aggregated data items.</td> 
</tr> 
<tr>
<td><a href="./files/Data_Items_Composition.md" target="_blank">Data items composition</a></td>
<td>Overview of the FHIR attributes used/needed in data items aggregation.</td>
</tr>
<tr>
<td><a href="./files/Dokumentation_Dashboard_Backend_v0_3_0a.pdf" target="_blank">Documentation of the FHIR implementation</a></td>
<td>A documentation of which data items are filled with which FHIR resources and attributes according to which logic.</td>
</tr>
<tr>
<td><a href="./README.md">Installation/Configuration guide</a></td>
<td>The installation and configuration instructions for this project</td>
</tr>
<tr>
<td><a href="./doc/index.html" target="_blank">Javadoc Files</a></td>
<td>The Javadoc files for this project</td>
</tr>
<tr>
<td><a href="./samples" target="_blank">Sample data</a></td>
<td>Example bundles to illustrate the expected resource structure and for test imports.</td>
</tr>
</table>

Not all items are currently supported by the DDP, a list of unsupported items can be
found [here](./data-items-support.md).

Important note: The DDP does not yet offer k-anonymity options, but may do so in the future.

## License

This project is released under the terms of [GPL version 3](LICENSE.md).

```
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```

## Requirements

The code is platform-independent and was tested on Linux and Windows environments.

The following software must be available to execute the program:

- Java 17 (JDK mandatory for compilation, OpenJDK recommended)
- Apache Maven (mandatory for compilation, optional if provided jar is executed)
- Dependencies ([utilities](https://www.github.com/mwtek/utilities)
  and [dashboardlogic](https://www.github.com/mwtek/dashboardlogic) repositories)

It is recommended to have both programs installed in a way that the correct versions of *java* and
*mvn* are found in the system path.

In addition, at least 4 GB of free RAM, preferably 8 GB, should be available. The size depends on
the number of resources that have to be retrieved from the FHIR server. In a test with ~ 500.000
resources, 4 GB was the lower limit.

## Requirements FHIR data

In order for the data set to be generated completely and correctly, the following FHIR resource
types are required:

- Observation (Disease-context-related laboratory codes, encoded in LOINC; results coded as <i>
  CodeableConcept</i>
  with codes from
  this <a href="https://simplifier.net/medizininformatikinitiative-modullabor/valuesetqualitativelaborergebnisse">
  MII value set</a>)
- Condition (Covid-19/Influenza diagnosis codes, encoded in ICD-10)
- Patient (Age/zip code/gender required)
- Encounter (Required, among other things, for admission and discharge times/types, as well as ICU
  stays)
- Location (for the detection of ICU wards)
- Procedure (for the detection
  of <a href="https://simplifier.net/medizininformatikinitiative-modul-intensivmedizin/valueset-code-procedure-beatmung-snomed">
  artificial respiration</a>
  and <a href="https://simplifier.net/medizininformatikinitiative-modul-intensivmedizin/valueset-code-extrakorporale-verfahren">
  ECMO procedures</a>)

The following diagram shows the workflow of the FHIR data query and the content of the respective
FHIR resources required for the Json transformation:

![Corona Dashboard - FHIR Data Retrieval](img/CoronaDashboard-FHIRDataRetrieval.png)

## FHIR search queries

The following is an overview of the approximate FHIR search requests sent from the processor to the
FHIR server:

| Resource    | FHIR Search Query                                                                | Comments                                         |
|-------------|----------------------------------------------------------------------------------|--------------------------------------------------|
| Condition   | [base]/Condition?code=U07.1&_pretty=false&_count=500                             |                                                  |
| Observation | [base]/Observation?code=94640-0,94306-8,96763-8,96895-8&_pretty=false&_count=500 |                                                  |
| Patient     | [base]/Patient?_id=1234,1235,1236                                                | IDs from Observation.subject + Condition.subject |
| Encounter   | [base]/Encounter?subject=1234,1235,1236                                          | IDs from Patient.subject                         |
| Procedure   | [base]/Procedure?code=233573008,243147009&subject=1234,1235,1236                 | IDs from Encounter.id                            |
| Location    | [base]/Location?_id=123,234,345                                                  | IDs from Encounter.location                      |

## Compatibility KDS profile versions

The KDS profiles are highly dynamic, and the current status quo of the profiles can differ
significantly from the development status of the Dashboard Processor. To cover future changes, we
have always worked with the current working versions in the Simplifier. The risk that profile
changes will also affect the workflow in the Dashboard Processor is small, but not impossible. The
following table provides an overview of which KDS profile versions were current at the time of
release and for which compatibility has been adequately tested. In case of problems and obvious
incompatibilities, please email the developer or open an issue. It is tried to respond to the
constant innovations by new Dashboard Processor versions.

| KDS Module                                                                                        | Profile(s)                                                                                                   | Minimum Version | Comments                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|---------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [KDS Person](https://simplifier.net/medizininformatikinitiative-modulperson)                      | [Patient](https://simplifier.net/medizininformatikinitiative-modulperson/sdmiipersonpatient)                 | 2024.0.0        |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| [KDS Fall](https://simplifier.net/medizininformatikinitiative-modulfall)                          | [Encounter](https://simplifier.net/medizininformatikinitiative-modulfall/kontaktgesundheitseinrichtung)      | 2024.0.0        | `Encounter.location` usage on `Versorgungsstellenkontakt` level is necessary (or alternative a flagging of icu providers via `Encounter.serviceProvider`) for the usage of data items that are based on transfer history (e.g. `current.treatmentlevel`). These resources must have a linkage to the `Einrichtungskontakt` encounter, either via the `Encounter.identifier.Aufnahmenummer` or indirectly, recursively via `Encounter.partOf` (after setting `use-part-of-instead-of-identifier` to `true` in the application.yaml). | 
| [KDS Labor](https://simplifier.net/medizininformatikinitiative-modullabor)                        | [Observation](https://simplifier.net/medizininformatikinitiative-modullabor/observationlab)                  | 1.0.6           |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| [KDS Diagnose](https://simplifier.net/medizininformatikinitiative-moduldiagnosen)                 | [Condition](https://simplifier.net/medizininformatikinitiative-moduldiagnosen/diagnose)                      | 2024.0.0        |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| [KDS ICU](https://simplifier.net/MedizininformatikInitiative-Modul-Intensivmedizin/~introduction) | [MII_Beatmung](https://simplifier.net/medizininformatikinitiative-modul-intensivmedizin/sd_mii_icu_beatmung) | 2024.0.0-alpha1 |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| [KDS Strukturdaten](https://simplifier.net/medizininformatikinitiative-modulstrukturdaten)        | [Location](https://simplifier.net/medizininformatikinitiative-modulstrukturdaten/sd_mii_struktur_location)   | 1.0             | Optional since the modul is still in the draft phase.                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |

## Runtime Configuration

The runtime configuration can be found in `src/main/resources/application.yaml`, it must be modified
according to the local setup. If the route is taken via the execution of a compiled .jar (see
chapter 'Program execution'), a copy of this file must also be located in the base directory.

The main adjustments that need to be made:

- Adjustment of the local location information in the "provider" area, which is part of the output
- Adaptation of the FHIR server endpoint to the local endpoint including authentication
  configuration
- Activation/Deactivation of the disease-context of which data items should be generated
    - To activate the generation of influenza data, for example, you must
      set `global.generate-influenza-data` to `true`.
- Exclude all data items (`data-items.excludes`) for which it cannot be ensured that the results are
  100% correct and complete, for instance because various basic data cannot yet be found in the FHIR
  server.

Optional adjustments:

- If a provider cannot serve various data items from the specification, these can be explicitly
  excluded here
- If LOINC/ICD/Procedure codes are to be replaced by internal code identifiers, this can be
  configured here (e.g. because the local mapping has not yet been completed)

The runtime configuration provides comments for all setup options.

## Program installation and execution

The program is set up as a Maven project and can either be built manually or by executing the
"build.sh" script. To use this application, the two libraries "utilities" and "dashboardlogic"
have to be cloned. It is also possible to run the program via a Docker container.

**Install and run via script**

* Create a new Folder where the three Repos are supposed to be located in.

```
mkdir dashboardprocessor
cd dashboardprocessor
```

* Clone the repos in there via:

```
git clone --branch v0.5.4-dev.0 https://www.github.com/mwtek/utilities.git
git clone --branch v0.5.4-dev.0 https://www.github.com/mwtek/dashboardlogic.git
git clone --branch v0.5.4-dev.0 https://www.github.com/mwtek/dashboarddataprocessor.git
```

* Go into "dashboarddataprocessor"

```
cd dashboarddataprocessor
```

* Execute "build.sh"

```
./build.sh
```

After installation, the project can be run via:

```
mvn spring-boot:run
```

or via using the run-script:

```
./run.sh
```

**Install and run manually**

For the Project to run without any issues, it is mandatory to install the other two libraries in a
certain order:

utilities &#8594; dashboardlogic &#8594; dashboarddataprocessor

First utilities, then dashboardlogic

```
mvn clean install
```

Then execute the main Project:
dashboarddataprocessor

```
mvn clean install
mvn spring-boot:run
``` 

**Executing a jar file**

**Important:** For this solution, a copy of the `application.yaml` must be created, copied to the
base folder and configured with the local settings. When the .JAR file is executed,
the `application.yaml` is taken from the base directory by default at runtime.

A precompiled file named `dashboarddataprocessor-0.5.4.jar` can be found in the project
target directory. Be aware that before you can access the .jar file, you have to successfully build
the project following one of the before mentioned approaches of 'Install and run via script' or '
Install and run manually'.

Execute this file (or the precompiled file accordingly):

```
java -jar target/dashboarddataprocessor-0.5.4.jar
```

Note that the settings must be adjusted in the .yaml file inside the packed .jar archive. This can
be done by changing the file inside the archive or moving a new settings file to this location
inside the archive, it is not necessary to recompile the code for changes to take effect.

**Executing a docker container - as Image**

We provided files for **creating DDP-Image** and using it with docker. There is a `build-docker.sh`
-script in `docker-image`-folder you can execute to
build DDP-Image on local Server.
The script will use the file `Dockerfile` in same directory to create the docker container. It is
needed to have the `dashboarddataprocessor-0.5.4.jar` and the `application.yaml` you want to use
on
the right place. By default, the created jar by `build.sh`-script in the target-folder is used and a
copy of the `application.yaml` in the dashboarddataprocessor-folder. You can adapt the used files by
editing following lines in `Dockerfile`.

```
COPY dashboarddataprocessor/target/dashboarddataprocessor-0.5.4.jar /dashboarddataprocessor/dashboard-data-processor.jar
COPY dashboarddataprocessor/application.yaml /dashboarddataprocessor
```

After the script has been finished, you can use the created container locally. Besides this the
file `DDP-V0.5.4.tar` is created, which contains the whole software. You can put it to any
server
you want to use for DDP. To deploy it you can use ansible-script `deploy-docker.yaml` which also
handles whole serversetup.

There is a `docker-compose.yml` provided as well, with which the deployed version can be started or
stopped via docker-compose.

**Executing a docker container - as local build**

We provided files for using **DDP with docker as local build**. There is a `docker-build`-folder
where you can run `docker-compose up -d` to build and run DDP on local server.
For usage, you have to copy both files from folder to the same folder level as the three
repository-folders.
Change all lines in both files which are marked as TODO before executing `docker-compose up -d`.

## Authentication FHIR Server

A total of three authentication methods are available to authenticate with the local FHIR
server: `BASIC`, `NONE` and `SSL`. As expected, the configuration takes place in `application.yaml`.

## Notes on the x509 authentication for the Rest API

[Here is a very helpful internet page on setting this up in a Spring boot application](https://www.baeldung.com/x-509-authentication-in-spring-security)

What you need to do boils down to this:

Generate a csr for your server or client, then your certificate authority can use this to generate a
signed certificate.

Then you should import your certificate and private key into a `.p12` file and then a .jks keystore
file.

You should also create a truststore.jks file which contains the root certificate (the certificate
authority)

You can then enter the correct information into your `application.yaml`. Check the current
configuration to see how you can set it up properly, but here is an explanation below:

```
x509-auth:
	keystore: #path to keystore.jks file
	keystore-password: #password to unlock the keystore file
	trust-store: #path to truststore.jks file
	trust-store-password: #password to unlock truststore file
```

No other configurations should be necessary in terms of making edits to the code, and you can find
all the bash commands to do the above-mentioned steps in the link above.

# Starting a process

The process of creating a Corona Dashboard JSON specification starts with a GET request to the local
server instance. It's recommended to use a client like Postman for this. A run can take several
minutes.

Note that the following example is based on the default configuration, the protocol (https when ssl
is used), authentication, hostname and port must be changed on a productive system.

Http-Request:

```
URL: http://localhost:9091/createJson
HTTP-Method: GET
Content-Type: application/json
Authentication-Method: BASIC
Username: user
Password: pwd
```

The response to this HTTP request should contain a fully completed dashboard specification with the
data from the local FHIR server. This can then be submitted via `PUT` to the dashboard backend as
usual. An automatic solution is currently being worked on.

## Parameterization options

It is possible to control the projects for which data is to be generated via a REST call. A
comma-separated list with at least one value from the value set [`covid`,`influenza`,`kiradar`] can
be transferred via the `scopes` parameter. If the parameter is not specified, the settings from the
`application.yaml` are used as usual.

Example call:

```
http://localhost:9091/createJson?scopes=covid,influenza,kiradar
```

# Installing a version upgrade

IMPORTANT: When using a new dashboard data processor version, please always use the
latest `application.yaml` file as template and put your previous local settings there.
It often happens that new entries or changes here have a big impact on the current version.

Please also note that when upgrading, the code of all three projects (`dashboardataprocessor`
, `dashboardlogic` and `utilities`) must be downloaded and installed.

# <a name="not-supported-items"></a>Not yet supported data items

The current state of which items can be supported and generated by the DDP can be
found [here](./data-items-support.md).

# Outlook

The number of parameterizable settings can be extended as desired (for example, if the
local FHIR resources have special characteristics). If there should be wishes in this regard, please
send mail to one of the developers or alternatively create an issue.

In addition, the <a href="./files/Documentation_Dashboard_Backend_v0_3_0a.pdf" target="_blank">
documentation of the FHIR implementation</a> still needs to be updated to version 0.5.4.

We have also started implementing a token-based fhir server authentication option. It is already
executable, but has not yet been tested intensively, so the chances are high that this option will
make it into an upcoming release soon.

Presumably, there will also soon be an option to switch FHIR search requests from GET to POST.

# Troubleshooting / Logging

If there are many problems in data retrieval, it may be useful to extend logging. It is possible to
output all FHIR search queries executed by the DDP in the console or to write them to a logging
file. For this the following code section in the `application.yaml` must look like this:

```
## Logging
logging:
  level:
    #  root: debug
    de.ukbonn.mwtek.dashboard: DEBUG
    # de.ukbonn.mwtek.dashboardlogic: DEBUG
    # de.ukbonn.mwtek.utilities: DEBUG
  file.name: dashboardprocessor.log
  file.max-size: 100MB
```

The `file.*` fields are optional, but allow the debug output in the console output to be written to
a local file, which by default is located in the top-level folder.

In addition, the `global.debug` field in `application.yaml` can be set to `true` to extend the JSON
output with items that reflect which encounter/patient (resource) IDs made up the individual data
items.

**Java VM Heap Size**

If you discover that the Java VM has too little RAM available (OutOfMemoryError: Java heap space),
you can try increasing the maximum heap size. For example, in this way:

```
java -jar -xMx 8G  target/dashboarddataprocessor-0.5.4.jar
```

**Connection to the FHIR server failed: 431 Request Header Fields Too Large**

For some (mostly parallel) FHIR search requests, lists with IDs (e.g. case number) are included as
input filters. If the FHIR server cannot handle the length of the list, it regularly reports a 431
message (Request Header Fields Too Large).

Solution: Reduction of the number of IDs given at the same time. This is controlled by
the `batchsize` parameter in `application.yaml`. The default value is 500, with a value of 1000 the
described behavior could be observed in our tests on a hapi machine but really depends on the
standard length of the IDs.

**Operation outcome: Request timed out after x ms**

With the HAPI server it could be observed that with certain data load, the creation of partial index
lists in the navigation via offset and count (e.g. the retrieval of all observation resources) can
take longer than one minute. The HAPI server apparently stops processing requests after one minute
by default if it cannot process the request in that time. s

Solution: Caching the data before retrieving it can be helpful in the short term (e.g.
via `?_summary=count`) on the corresponding resource. Otherwise, this timeout value would have to be
increased (configuration varies depending on the FHIR server).

**We do not use any location resources at our site. Can we still annotate encounter as ICU cases?**

Yes, if you have a local list with IDs of ICU organizational units, and you use the
`Encounter.serviceProvider` attribute for the annotation, these can be used alternatively. To do
this, a comma-separated list of IDs must be entered in `application.yaml`
under `global.service-provider-identifier-of-icu-locations`. This list
works additionally, any `Encounter.location` entries are also checked. Only the encounter at the
lowest level (`Versorgungsstellenkontakt`) is checked.
In addition, the `Encounter.type` is checked for slice `kontaktart` = `intensivstationaer` if no
location references can be found within the Encounter. This offers another possibility to annotate
supply contacts as ICU stays.

# Contributors

Thanks to everyone who contributed to this project:

- [haemka](https://github.com/haemka)
- [FloSeidel](https://github.com/FloSeidel)
- [schwzr](https://github.com/schwzr)
- [UMEihle](https://github.com/UMEihle)
- [KutSaleh](https://github.com/KutSaleh)
