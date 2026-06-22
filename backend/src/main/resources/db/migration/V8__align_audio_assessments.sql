-- V8 — align audio_assessments table with Java entity
-- Entity has rubric_id and curated_context but SQL doesn't.
-- SQL has term_id NOT NULL but entity doesn't map it yet.

-- add rubric FK (required by AudioAssessment.java)
ALTER TABLE audio_assessments ADD COLUMN rubric_id UUID;
ALTER TABLE audio_assessments ADD CONSTRAINT fk_audio_rubric
    FOREIGN KEY (rubric_id) REFERENCES knec_rubrics(rubric_id) ON DELETE RESTRICT;

-- add curated_context (entity field, maps to teacher's custom instructions for now)
ALTER TABLE audio_assessments ADD COLUMN curated_context TEXT;

-- make term_id nullable until we properly link it through the entity
ALTER TABLE audio_assessments ALTER COLUMN term_id DROP NOT NULL;
