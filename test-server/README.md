# Local Consent Tester

Simple web server to test the Smart4Health Data-provision Consent and Signing flow locally in the browser, instead of
having to start up a mobile emulator.

First install deps with `npm install`.

To run, simply run:

```
npm run start
```

Then, when you start the consent flow, make sure your `successRedirectUrl` query param is
`successRedirectUrl=http://localhost:4444/consents/{consentId}/documents/create`
