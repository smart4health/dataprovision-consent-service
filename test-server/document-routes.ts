import express from 'express';
import axios from 'axios';
import {appState} from './app-state';
import {config} from './config';

interface CreateDocumentOption {
    optionId: number;
    consented: boolean;
}

interface CachePDFResponseBody {
    documentId: string;
    token: string;
}

class JwtStore {
  private tokens = [
    `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkxvbGxlciBMb2xsaXB1cyIsImlhdCI6MTUxNjIzOTAyMiwiaXNzIjoibG9sIn0.fxtyhfcVq50UlIUsbl2eGCzTYuES9ZENQa6-YlzV1M4`,
    `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJpc3MiOiJsb2wifQ.aEMhsJmkF-KFZdyq6rjgBLR6Tp7xRantj-ucnOlalRg`,
    `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlJvb25kb2cgTWNKb25lcyIsImlhdCI6MTUxNjIzOTAyMiwiaXNzIjoibG9sIn0.9ZNTaL-2IjmGBnPLTz1H8XAmIiY90wAeUlu-tbrGQ-Y`,
    `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6Ik9ybGFuZG8gTWNDaGlja2VuTnVnZ2V0cyIsImlhdCI6MTUxNjIzOTAyMiwiaXNzIjoibG9sIn0.tX-ALozsY7DdjXK7PiwOU7NbodtUNORIeGyOKDSJMfI`,
    `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkJvYmxpc2kgQmliYmx5Ym9vcCIsImlhdCI6MTUxNjIzOTAyMiwiaXNzIjoibG9sIn0.WVkHIp9bs9qM77kkAC42wtHnnx09ggk1aPgqp5KzcPQ`,
    `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6Ik1vcHBsZSB0aGUgV2hhbGUiLCJpYXQiOjE1MTYyMzkwMjIsImlzcyI6ImxvbCJ9.fh5Og-kiEX0OvcplLfnx7fVnDu3su6nrtmotMOwp6hI`
  ];

  private lastToken?: String;

  // @ts-ignore
  public getRandomToken = () => {
    let newToken = false;
    while (!newToken) {
      const nextToken = this.tokens[Math.floor(Math.random() * this.tokens.length)]
      if (nextToken !== this.lastToken) {
        this.lastToken = nextToken;
        return nextToken;
      }
    }
  }
}

export class DocumentRoutes {
    private static jwtStore = new JwtStore();

    public static createDocument = async (req: express.Request, res: express.Response) => {
        try {
            const {consentId} = req.params;
            const options = req.query.options as string;
            if (!options) {
                throw new Error("Must provide options in query string");
            }

            const parsed = JSON.parse(Buffer.from(options, "base64").toString("utf-8")) as CreateDocumentOption[];
            appState.options = parsed;

            console.info("Creating PDF...")
            const createPDFResponse = await axios.post(`${config.consentBackend}/api/v1/consents/${consentId}/documents`, {
                options: parsed
            });
            if (createPDFResponse.status >= 400 && createPDFResponse.status < 600) {
                console.error(`Error creating PDF`, createPDFResponse.data)
            }
            const pdf = createPDFResponse.data as String;

            console.info("Caching PDF...")
            const cachePDFResponse = await axios.post(`${config.consentBackend}/api/v1/signatures`, pdf, {
                headers: Object.assign({
                    'Authorization': `Bearer ${DocumentRoutes.jwtStore.getRandomToken()}`,
                    'X-Hmx-Success-Redirect-Url': 'http://success.test',
                    'X-Hmx-Cancel-Redirect-Url': 'http://cancel.test',
                    'X-Hmx-Consent-Id': consentId,
                    'X-Hmx-Review-Consent-Url': `http://localhost:4444/consents/${consentId}/review`,
                    'Content-Type': 'application/pdf'
                }, config.platform ? {
                    'X-Hmx-Platform': config.platform,
                } : null)
            });
            const cachePDFResponseData = cachePDFResponse.data as CachePDFResponseBody;

            const {token, documentId} = cachePDFResponseData;

            console.info("Redirecting now to signature form...")
            console.info(`documentId=${documentId}`)
            return res.redirect(`${config.signingBackend}/signatures/${documentId}/sign?token=${encodeURIComponent(token)}`);
        } catch (error) {
            console.error(`Error: ${error}`);
            res.json({ok: false});
        }
    }

    public static reviewConsent = async (req: express.Request, res: express.Response) => {
        try {
            const {consentId} = req.params;
            const {options} = appState;

            if (!options) {
                throw new Error(`
          You are probably using the review consent URL without having clicked it from the signature page, where the state
          gets base64-encoded so that it can then be used to pre-hydrate the state of the consent flow.
        `);
            }

            const successRedirectUrl = `http://localhost:4444/consents/${consentId}/documents/create`;
            const cancelRedirectUrl = `http://cancel.test`;

            const state = encodeURIComponent(Buffer.from(JSON.stringify(appState), "utf-8").toString("base64"));
            return res.redirect(`${config.consentBackend}/consents/${consentId}?successRedirectUrl=${successRedirectUrl}&cancelRedirectUrl=${cancelRedirectUrl}&state=${state}`);
        } catch (error) {
            console.error(`Error: ${error}`);
            res.json({ok: false});
        }
    }
}