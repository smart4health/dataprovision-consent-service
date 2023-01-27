# Smart4Health Data-provision Consent Service

This service takes care of guiding the citizen through the consent process in
the [Smart4Health MyScience App](https://github.com/smart4health/my-science-app). The consent resources are pulled as an
external configuration
from [dataprovision-consent-resources](https://github.com/smart4health/dataprovision-consent-resources).

## Modules

- api - bootstrapping logic to start the actual API server
- signing - service to manipulate and sign PDFs using PDFBox
- signing-javascript - frontend code for the signing flow
- persistence - persistence layer
- consent - service to generate frontend informed consent flows based on configuration files hosted remotely
- consent-javascript - frontend code for the consent flow
- commons - shared backend code

## Local deployment

Without any set spring profile the service will use an in-memory database and local static resources for the consent and
signings. See
[consent/src/main/consents](consent/src/main/resources/static/consents)
[signing/src/main/signings](signing/src/main/resources/static/signings)

These consents do not reflect the application's behavior as it would on the deployed services. Consider providing them
as external resource as well. To do this, use profile `local-repositories`:

`SPRING_PROFILES_ACTIVE=local-repositories ./gradlew bootRun`

To get rid of the in-memory DB and use postgres, also use profile `postgres` and
[run DB locally](#persistence-on-local-postgres-db)

### Persistence on local postgres db

To set up the local postgres database, start up a postgres container with

```shell script
docker run --rm -it --name postgres \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_USER=username \
  -e POSTGRES_DB=dynamic-consent \
  -p 5432:5432 postgres
```

Remember to use additional run profile `postgres`.

### Consent frontend via test-server

The consent frontend was typically run within the context of a mobile application, and used to require an emulator to
use the full flow. The `test-server` folder is a simple server that mimics the functionality of the mobile apps so that
the flow is fully testable within the browser (that is, moving between consent and signing).

To run it, navigate to the `test-server` folder and run:

```
npm install
npm start
```

The server will listen on port `4444`.

### Consent / Signing resources

#### Remote

The consent and signing resources are being fetched remotely and can be changed in
the [application.yaml](api/src/main/resources/application.yaml) at `remote-consent.sources.smart4health`
and `remote-signing.sources.smart4health`

Location: https://consents.smart4health.eu/

#### Local

In case you want to use custom external resources served locally:

1) clone the respective
   repo [dataprovision-consent-resources](https://github.com/smart4health/dataprovision-consent-resources)
2) use `serve` or an equivalent tool to host the static content locally at a custom port
3) adapt host in [application.yaml](api/src/main/resources/application.yaml) at `local-repositories` profile settings

## Secrets

There are three kinds of secrets engines used within this service depending on their projects use, as well as a mock
variant. Their usage is mutually exclusive and is enabled by using the spring profile:

- `secrets-aws` (AWS Secrets Manager): Used for S4H development instances hosted on AWS
- `secrets-vault` (HashiCorp Vault): Used for S4H production instance hosted on OpenStack
- none: If none of these are desired (`default` spring profile),
  the `com.healthmetrix.dynamicconsent.commons.MockSecrets`
  can be used for local development.

## Hashicorp Vault local deployment

To run Vault locally and let consent use approle authentication using sample secrets, proceed the following:

1. Start Vault dev server locally (dev mode skips unsealing and some defaults):

```shell
docker run --rm -p 8200:8200 --cap-add=IPC_LOCK --name=vault-dev -e 'VAULT_DEV_ROOT_TOKEN_ID=root' vault
```

2. Run this custom set up script for the initial token, policies, roles and secrets. Also starts the service which will
   use the initial token to renew the approle lease every 20 seconds and using full pull mode to fetch the role-id and
   secret-id:

```shell
SPRING_CLOUD_VAULT_TOKEN=$(sh vault_local.sh | tail -1) SPRING_PROFILES_ACTIVE=secrets-vault ./gradlew bootRun
```

## Others

### Adding a new PDF

When a new PDF needs to be loaded, we need to calculate the coordinates of the various checkboxes and inputs to insert
text at. To do this, go to the [following url](https://pdfbox.apache.org/download.cgi) and download the first command
line tool (pdfbox-app-<VERSION>.jar). Then, load a specific PDF with the following command:

```shell script
java -jar <path/to/pdfbox-app.jar> PDFDebugger <path/to/pdf>
```

You can then page through the PDF and hover over spots to find out the coordinates. Sometimes the coordinates are not
100% accurate, and you need to play around with them.

For that check the SignPdfTask in build-logic/local-driver. Make sure dynamic-consent is running locally before, and
that the consent sources are served locally, see [Consent / Signing resources -> Local](#local).

**Important**: Make sure to test that not only the signature is in the pdf, but also the texts (first name, last name
and date). We noticed when using PDF version 1.3 (with maybe some weird form overlays still in the pdf) for the source
PDF, some viewers, e.g. Apple Preview, Evince (Default for GNOME, Ubuntu) do NOT show these texts. Others, e.g. Adobe
Acrobat or the Chrome-builtin viewer displayed them.
Failsafe way is always export the PDF as PDF/A or ISO compliant one with PDF version 1.5 or higher.

### local entry points for consent flow

Smart4Health (only available using `local-repositories` profile):
http://localhost:8080/consents/smart4health-research-consent-en?successRedirectUrl=...

For urlParams descriptions see [GUIDE](GUIDE.md)

### Generating a schema of the Consent Template Structure

There are three yaml template files that are read from the remote repository:

- `{consent-dir}/template.yaml`: deserialized
  to [consent.templating.ConsentTemplate](consent/src/main/kotlin/com/healthmetrix/dynamicconsent/consent/templating/ConsentTemplate.kt)
- `{consent-dir}/config.yaml`: deserialized
  to [commons.pdf.ConsentPdfConfig](commons/src/main/kotlin/com/healthmetrix/dynamicconsent/commons/pdf/ConsentPdfConfig.kt)
- `{consent-dir}/signing/template.yaml`: deserialized
  to [signing.ConsentSigningTemplate](signing/src/main/kotlin/com/healthmetrix/dynamicconsent/signing/ConsentSigningTemplate.kt)

To generate a schema to be used by s4h-consents repo for validation purposes, just run:

`./gradlew cleanTest :api:test --tests com.healthmetrix.dynamicconsent.consent.api.GenerateConsentSchema`

The files will be written to [api/build/json-schema](api/build/json-schema).

Minor adjustments for non-optional fields that have default values have to be done though. See the example json in
[https://github.com/smart4health/dataprovision-consent-resources/tree/main/_schema](https://github.com/smart4health/dataprovision-consent-resources/tree/main/_schema)
. That's because jackson-kotlin marks it as required: https://github.com/mbknor/mbknor-jackson-jsonSchema/issues/97.