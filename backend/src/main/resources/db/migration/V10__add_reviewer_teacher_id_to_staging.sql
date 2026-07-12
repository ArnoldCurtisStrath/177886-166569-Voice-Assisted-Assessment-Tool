-- V10: Add reviewer_teacher_id to staging_assessments for teacher review tracking
ALTER TABLE staging_assessments ADD COLUMN IF NOT EXISTS reviewer_teacher_id UUID REFERENCES teachers(teacher_id);
