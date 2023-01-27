# Smart4Health Data-provision Consent Service - Integration Guide

This document will describe the flow through the consent and signing services, which apps can implement to manage the
consent of users.

As a general overview, the consent document is read and configured by the user, then, the user signs the document.

## Consent service

### Choose a consent document

The first step is to decide which consent the user may want to sign. As of writing, there are only two possible consent
documents, and thus two possible
`consentId`s, but in the future there may be more, as well as new `consentId`s that override older versions of the
documents they represent.

Possible IDs at time of writing:

- `smart4health-research-consent-en`: Smart4Health consent for research data provision

Apps may choose to let the user select from a set of possible IDs, or may intend to only use one ID, in which case
choosing is unnecessary.

Important! The format of the consentId is `smart4health-name`. This must match the injected ENV vars or the ones
specified in  [application.yaml](api/src/main/resources/application.yaml) at `remote-consent.sources.smart4health`.

### Present the document to the user

This step takes place in the browser. Redirect the user like so

```
GET /consents/{consentId}?successRedirectUrl={redirectUrl}&cancelRedirectUrl={cancelRedirectUrl}&platform={platform}
```

This `GET` request is only a representation of what the browser should execute. On Android, for example, the full
url `https://consent.dev.healthmetrix.com/consents/...`
could be opened in a Chrome Custom Tab.

When the user has read, configured, and consented to the document, they are redirected to the given `successRedirectUrl`
with an additional `options` query parameter containing a Base 64 encoded JSON blob representing the selection consent
options. These decoded options will be needed in the next step.

> Note: It is recommended that the `redirectUrl` uses a unique scheme and host, for example
> `com.healthmetrix.researchapp://consent-done`. This greatly reduces the chances
> of the redirect being caught by a different app on the Android platform

#### Inputs

- `consentId`: which document to be presented, as in [Choose a consent document](#choose-a-consent-document)
- `successRedirectUrl`: Where to redirect the user to when the configuration of the document is complete
- `cancelRedirectUrl` (optional): Sometimes the flow needs to be cancelled and certain buttons eject the user from the
  flow. This URL is the destination the user is taken to when cancelling the flow. The reason will be provided as a
  query parameter to the destination URL. For instance: `https://<cancel-redirect-url>?reason=quiz` (if the user failed
  the provided the quiz at the end).
- `platform` (optional): This query parameter is necessary for platform-specific styles (i.e. Apple-specific fonts) and
  for sharing the consent document at the end. Possible values are `web`, `android`, and `ios`. If the flow is consumed
  by an Android or iOS app, a function injected into the javascript can be invoked this way. In Android, this function
  should be called `shareDocument` and lives on a global `Android` object that should also be injected, and will get
  invoked when the user clicks on the `share` button on the last page of the consent flow. It accepts a base-64 encoded
  version of a JSON stringified array of options. In iOS, the message handler is called `shareHandler`.
- `state` (optional): This parameter is completely optional and is only used when coming back to the consent flow from
  somewhere else, after completing it. The options array contained within is the output from the consent flow when the
  user first goes through it. This parameter should be a JSON-stringified and base-64 encoded version of the state,
  whose deserialized type signature looks like follows:

  ```
  {
    options: [
      { optionId: Integer, consented: Boolean }
    ]
  }
  ```

#### Outputs

- `options`: A query parameter on `successRedirectUrl` containing a string representing the options the user selected.
  When the flow is complete, the user is redirected to `successRedirectUrl?options=...`

### Download the configured document

After receiving the consent options, download the configured (but not signed) document by calling

```
POST /api/v1/consents/{consentId}/documents
Content-Type: application/json

{"options": <options>}
```

#### Inputs

- `consentId`: The same consent id as in [the previous step](#present-the-document-to-the-user)
- `<options>`: The Base 64 decoded output of [the previous step](#present-the-document-to-the-user), as the request
  body. It should decode to valid JSON

#### Outputs

If successful, the service will return `201` and the response body will contains the bytes of the configured document as
a PDF.

> Note: it is recommended to cache the PDF in storage, as the document may be too large
> for some mobile devices to easily keep in memory

### Prepare for signing

After caching the configured document, the signing service needs some information to prepare for signing the document.
Namely, it needs the configured document, the consent type, and several redirect urls. The
`cancelRedirectUrl` exits the signing flow when the user wishes to cancel the entire consent and signing flow. The
`reviewConsentUrl` is a URL back to somewhere that prepares to reload the consent flow with an additional query
param `state`, which is a JSON-stringified and base-64 encoded version of the
state. [See the query parameters available for the `GET`
route loading the consent flow for more options on the `state` param](#present-the-document-to-the-user).

```
POST /api/v1/signatures
Authorization: Bearer {token}
X-Hmx-Success-Redirect-Url: {successRedirectUrl}
X-Hmx-Consent-Id: {consentId}
X-Hmx-Cancel-Redirect-Url: {cancelRedirectUrl}
X-Hmx-Review-Consent-Url: {reviewConsentUrl}
Content-Type: application/pdf

{document}
```

Access to the signing service is controlled by the bearer token in the `Authorization` header. As time of writing, it
only supports firebase user tokens from the research app project, but will be expanded in the future.

#### Inputs

- `consentId`: The same consent id as in [the previous step](#download-the-configured-document)
- `successRedirectUrl`: Where to redirect the user after they have signed the document, similar
  to [step 2](#present-the-document-to-the-user)
- `token`: Firebase user token
- `document`: The bytes of the pdf to be signed, as a request body

#### Outputs

If successful, a 201 response is returned, containing a JSON object with two fields

- `documentId`: The identifier for the uploaded document
- `token`: A new token used for identifying the user

### Sign

To sign the document, redirect the user to the following path, similar to [step 2](#present-the-document-to-the-user)

Make sure to URL encode the token.

```
GET /signatures/{documentId}/sign?token={token}
```

#### Inputs

- `documentId`: The id returned from [Prepare for signing](#prepare-for-signing)
- `token`: The URL-encoded token returned from [Prepare for signing](#prepare-for-signing)

#### Outputs

After signing, the user will be redirected to the given `successRedirectUrl` from
the [previous step](#prepare-for-signing)

### Download signed pdf

If the user has signed, this call can be made to fetch the final, signed pdf.

```
GET /api/v1/signatures/{documentId}
Authorization: Bearer {token}
```

#### Inputs

- `documentId`: As in [Prepare for signing](#prepare-for-signing)
- `token`: The authorization token, as in [Prepare for signing](#prepare-for-signing)

#### Outputs

If the user has signed, and a valid token is provided, the service will respond with `200`, and the response body will
contain the bytes of the signed document.

### Linking?
