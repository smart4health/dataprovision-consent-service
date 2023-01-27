CREATE TABLE linked_consents
(
    consent_flow_id VARCHAR NOT NULL PRIMARY KEY,
    chdp_id VARCHAR NOT NULL,
    linked_at TIMESTAMP NOT NULL
);