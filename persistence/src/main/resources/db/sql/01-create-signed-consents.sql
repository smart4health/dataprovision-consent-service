CREATE TABLE signed_consents
(
    consent_flow_id VARCHAR NOT NULL PRIMARY KEY,
    document_id VARCHAR UNIQUE NOT NULL,
    first_name VARCHAR NOT NULL,
    family_name VARCHAR NOT NULL,
    signed_at TIMESTAMP NOT NULL
);