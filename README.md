[![Deploy to dev and prod](https://github.com/navikt/pale-2-regler/actions/workflows/deploy.yml/badge.svg)](https://github.com/navikt/pale-2-regler/actions/workflows/deploy.yml)

# pale-2-regler
This project contains just the rules for validating legeerklæringer from pale-2 (https://github.com/navikt/pale-2)

## FlowChart
This the high level flow of the application
```mermaid
  graph LR
      pale-2 --- pale-2-regler
      pale-2-regler --- PDL
      pale-2-regler --- BTSYS
      pale-2-regler --- syfohelsenettproxy
      pale-2-regler --- azureAD;
```

## Technologies used
* Kotlin
* Gradle
* Ktor
* Jackson
* Junit

#### Requirements

* JDK 17


#### Build and run tests
To build locally and run the integration tests you can simply run
``` bash
./gradlew shadowJar
```
or on windows 
`gradlew.bat shadowJar`

#### Creating a docker image
Creating a docker image should be as simple as `docker build -t pale-2-regler .`

#### Running a docker image
``` bash
docker run --rm -it -p 8080:8080 pale-2-regler
```

### Upgrading the gradle wrapper
Find the newest version of gradle here: https://gradle.org/releases/ Then run this command:

``` bash
./gradlew wrapper --gradle-version $gradleVersjon
```

<!-- RULE_MARKER_START -->
Lege suspensjon
```mermaid
graph TD
    root(BEHANDLER_SUSPENDERT) -->|Yes| root_BEHANDLER_SUSPENDERT_INVALID(INVALID):::invalid
    root(BEHANDLER_SUSPENDERT) -->|No| root_BEHANDLER_SUSPENDERT_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;
```
HPR
```mermaid
graph TD
    root(BEHANDLER_IKKE_GYLDIG_I_HPR) -->|Yes| root_BEHANDLER_IKKE_GYLDIG_I_HPR_INVALID(INVALID):::invalid
    root(BEHANDLER_IKKE_GYLDIG_I_HPR) -->|No| root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR(BEHANDLER_MANGLER_AUTORISASJON_I_HPR)
    root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR(BEHANDLER_MANGLER_AUTORISASJON_I_HPR) -->|Yes| root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_INVALID(INVALID):::invalid
    root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR(BEHANDLER_MANGLER_AUTORISASJON_I_HPR) -->|No| root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR(BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR)
    root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR(BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR) -->|Yes| root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR_INVALID(INVALID):::invalid
    root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR(BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR) -->|No| root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_PS_I_HPR_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;
```
Validation
```mermaid
graph TD
    root(PASIENT_YNGRE_ENN_13) -->|Yes| root_PASIENT_YNGRE_ENN_13_INVALID(INVALID):::invalid
    root(PASIENT_YNGRE_ENN_13) -->|No| root_PASIENT_YNGRE_ENN_13_UGYLDIG_ORGNR_LENGDE(UGYLDIG_ORGNR_LENGDE)
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_ORGNR_LENGDE(UGYLDIG_ORGNR_LENGDE) -->|Yes| root_PASIENT_YNGRE_ENN_13_UGYLDIG_ORGNR_LENGDE_INVALID(INVALID):::invalid
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_ORGNR_LENGDE(UGYLDIG_ORGNR_LENGDE) -->|No| root_PASIENT_YNGRE_ENN_13_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR)
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR) -->|Yes| root_PASIENT_YNGRE_ENN_13_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_INVALID(INVALID):::invalid
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR) -->|No| root_PASIENT_YNGRE_ENN_13_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;
```

<!-- RULE_MARKER_END -->

### Contact

This project is maintained by [navikt/teamsykmelding](CODEOWNERS)

Questions and/or feature requests? Please create an [issue](https://github.com/navikt/pale-2-regler/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)