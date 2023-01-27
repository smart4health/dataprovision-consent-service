import express from 'express';
import { config } from './config';
import { DocumentRoutes } from './document-routes';

export class Server {
  app: express.Express;
  constructor() {
    this.app = express();
    this.app.get("/consents/:consentId/documents/create", DocumentRoutes.createDocument);
    this.app.get("/consents/:consentId/review", DocumentRoutes.reviewConsent);
      
  }

  public listen = (port: number) => {
    const server = this.app.listen(port);
    console.log(`Listening on port ${port}. Example entry pages`);
    console.log(`
      [smart4health-research-consent-en]: ${config.consentBackend}/consents/smart4health-research-consent-en?successRedirectUrl=http://localhost:${port}/consents/smart4health-research-consent-en/documents/create&cancelRedirectUrl=https://google.com
    `);
    return server;
  }
}
