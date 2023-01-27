CREATE TABLE cached_consents
(
    consent_flow_id VARCHAR UNIQUE NOT NULL PRIMARY KEY,
    document_id VARCHAR NOT NULL,
    consent_id VARCHAR NOT NULL,
    document BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL
);