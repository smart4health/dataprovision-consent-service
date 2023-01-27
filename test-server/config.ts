export const config = {
  consentBackend: process.env.CONSENT_BACKEND_URL || "http://localhost:8080",
  signingBackend: process.env.SIGNING_BACKEND_URL || "http://localhost:8080",
  platform: process.env.PLATFORM || null
};