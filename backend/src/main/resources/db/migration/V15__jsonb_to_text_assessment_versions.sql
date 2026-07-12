-- V15: Convert assessment_versions.json_data from JSONB to TEXT
-- The entity maps this as a String with columnDefinition="TEXT",
-- but V1 created it as JSONB. This causes a type mismatch error
-- when Hibernate tries to insert a VARCHAR into a JSONB column.
-- Same fix as V11 for staging_assessments.parsed_json_payload.

ALTER TABLE assessment_versions
    ALTER COLUMN json_data TYPE TEXT;
