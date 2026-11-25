-- Existing deployments can add this optional permission without changing their table definitions.
INSERT INTO ref_consent_type (consent_type_id,consent_code,consent_name,purpose,allows_employment_followup,is_required)
SELECT 9,'PROVIDER_CONTACT_SHARING','Share contact details with training providers',
       'Share email and phone with providers for training batches the trainee has joined',0,0
WHERE NOT EXISTS (SELECT 1 FROM ref_consent_type WHERE consent_code='PROVIDER_CONTACT_SHARING');
