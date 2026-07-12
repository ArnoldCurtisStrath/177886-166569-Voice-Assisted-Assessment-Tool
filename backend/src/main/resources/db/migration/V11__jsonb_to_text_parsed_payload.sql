-- V11: Convert parsed_json_payload from JSONB to TEXT to match JPA entity mapping.
-- Hibernate binds null values as VARCHAR, which PostgreSQL won't cast to JSONB.
ALTER TABLE staging_assessments ALTER COLUMN parsed_json_payload TYPE TEXT;
