ALTER TABLE signed_consents
    ADD COLUMN consent_id VARCHAR NULL;

UPDATE signed_consents
SET consent_id = (
    SELECT consent_id
    FROM cached_consents
    WHERE cached_consents.consent_flow_id = signed_consents.consent_flow_id
);

ALTER TABLE signed_consents
    ALTER COLUMN consent_id SET NOT NULL;