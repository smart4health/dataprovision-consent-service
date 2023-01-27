CREATE TABLE consent_flows
(
    consent_flow_id VARCHAR NOT NULL PRIMARY KEY,
    external_ref_id VARCHAR NOT NULL,
    consent_id VARCHAR NOT NULL,
    withdrawn_at TIMESTAMP NULL,
    signed_at TIMESTAMP NULL,
    UNIQUE(external_ref_id, consent_id, withdrawn_at)
);

INSERT INTO consent_flows (consent_flow_id,external_ref_id, consent_id, withdrawn_at, signed_at)
SELECT consent_flow_id,consent_flow_id,consent_id, withdrawn_at, signed_at FROM signed_consents;

INSERT INTO consent_flows (consent_flow_id,external_ref_id, consent_id)
SELECT consent_flow_id,consent_flow_id,consent_id FROM cached_consents
ON CONFLICT DO NOTHING;

ALTER TABLE signed_consents ADD CONSTRAINT signed_consents_fkey FOREIGN KEY(consent_flow_id) REFERENCES consent_flows(consent_flow_id) ON DELETE CASCADE;
ALTER TABLE cached_consents ADD CONSTRAINT cached_consents_fkey FOREIGN KEY(consent_flow_id) REFERENCES consent_flows(consent_flow_id) ON DELETE CASCADE;
ALTER TABLE linked_consents ADD CONSTRAINT linked_consents_fkey FOREIGN KEY(consent_flow_id) REFERENCES consent_flows(consent_flow_id) ON DELETE CASCADE;

ALTER TABLE signed_consents
    ALTER COLUMN signed_at DROP NOT NULL,
    ALTER COLUMN withdrawn_at DROP NOT NULL
