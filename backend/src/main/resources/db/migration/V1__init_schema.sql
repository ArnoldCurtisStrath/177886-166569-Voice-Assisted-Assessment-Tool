-- ==============================================================================
-- VoiceAssess V1 — Initial PostgreSQL schema
-- Mirrors the corrected DBML: separate profile tables, M:N junctions,
-- unique constraints, proper cascade/restrict/set-null FK rules.
-- ==============================================================================

-- Enable UUID generation (PostgreSQL built-in)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==============================================================================
-- CUSTOM ENUM TYPES
-- ==============================================================================
CREATE TYPE user_role AS ENUM ('ADMIN', 'TEACHER', 'PARENT', 'STUDENT');
CREATE TYPE job_status AS ENUM ('PROCESSING', 'COMPLETED', 'FAILED');
CREATE TYPE assessment_status AS ENUM ('pending', 'processing', 'completed', 'rejected', 'appealed');
CREATE TYPE staging_status AS ENUM ('processing', 'requires_review', 'resolved');
CREATE TYPE appeal_status AS ENUM ('pending', 'resolved', 'rejected');

-- ==============================================================================
-- 1. CENTRALIZED AUTHENTICATION & SECURITY
-- ==============================================================================
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tracks every login attempt for security auditing
CREATE TABLE authentication_logs (
    log_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    attempt_status VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 2. MULTI-TENANCY ANCHOR
-- ==============================================================================
CREATE TABLE schools (
    school_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_name VARCHAR(255) NOT NULL,
    knec_code VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 3. USER PROFILES (separate tables, linked to users via FK)
-- ==============================================================================
CREATE TABLE administrators (
    admin_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    school_id UUID NOT NULL REFERENCES schools(school_id) ON DELETE RESTRICT,
    full_name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(100) UNIQUE NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50),
    capability_array JSONB NOT NULL DEFAULT '[]'
);

CREATE TABLE teachers (
    teacher_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    school_id UUID NOT NULL REFERENCES schools(school_id) ON DELETE RESTRICT,
    full_name VARCHAR(255) NOT NULL
);

CREATE TABLE parents (
    parent_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50)
);

-- Students have school_id (not parent_id — that's M:N now via student_parents)
CREATE TABLE students (
    student_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    school_id UUID NOT NULL REFERENCES schools(school_id) ON DELETE RESTRICT,
    full_name VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL
);

-- M:N Parent-Student junction (FIX #1 — replaced students.parent_id 1:N)
CREATE TABLE student_parents (
    link_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES students(student_id) ON DELETE CASCADE,
    parent_id UUID NOT NULL REFERENCES parents(parent_id) ON DELETE CASCADE,
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, parent_id)
);

-- ==============================================================================
-- 4. ACADEMIC STRUCTURE
-- ==============================================================================
CREATE TABLE academic_terms (
    term_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(school_id) ON DELETE RESTRICT,
    term_name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active'
);

-- Classes exist independently of terms (FIX #2 — removed term_id from classes)
CREATE TABLE classes (
    class_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(school_id) ON DELETE RESTRICT,
    grade_level INTEGER NOT NULL,
    stream_name VARCHAR(50) NOT NULL
);

CREATE TABLE subjects (
    subject_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subject_name VARCHAR(150) NOT NULL,
    grade_level INTEGER NOT NULL
);

CREATE TABLE knec_rubrics (
    rubric_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subject_id UUID NOT NULL REFERENCES subjects(subject_id) ON DELETE RESTRICT,
    competency_desc TEXT NOT NULL,
    strand VARCHAR(150) NOT NULL,
    sub_strand VARCHAR(150) NOT NULL,
    rating_scale VARCHAR(50) NOT NULL
);

-- ==============================================================================
-- 5. M:N ASSIGNMENTS (Required for CBC Normalization)
-- ==============================================================================
CREATE TABLE teacher_class_assignments (
    assignment_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    teacher_id UUID NOT NULL REFERENCES teachers(teacher_id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(class_id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (teacher_id, class_id)
);

CREATE TABLE teacher_subject_assignments (
    assignment_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    teacher_id UUID NOT NULL REFERENCES teachers(teacher_id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(subject_id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (teacher_id, subject_id)
);

CREATE TABLE student_enrollments (
    enrollment_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES students(student_id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(class_id) ON DELETE CASCADE,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, class_id)
);

-- ==============================================================================
-- 6. CORE ASSESSMENT PIPELINE (Transactions & AI)
-- ==============================================================================

-- The hub of the entire assessment flow.
-- term_id captures which academic term this assessment belongs to (FIX #2).
CREATE TABLE audio_assessments (
    audio_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    teacher_id UUID NOT NULL REFERENCES teachers(teacher_id) ON DELETE RESTRICT,
    class_id UUID NOT NULL REFERENCES classes(class_id) ON DELETE RESTRICT,
    subject_id UUID NOT NULL REFERENCES subjects(subject_id) ON DELETE RESTRICT,
    term_id UUID NOT NULL REFERENCES academic_terms(term_id) ON DELETE RESTRICT,
    file_reference VARCHAR(500),
    topic VARCHAR(255),
    date DATE,
    target_sub_strands JSONB,
    custom_instructions TEXT,
    upload_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status assessment_status NOT NULL DEFAULT 'pending'
);

-- Async job tracking per audio upload.
-- FIX #3: unique (audio_id, job_type) — allows multiple job types per audio.
CREATE TABLE pending_audio_jobs (
    job_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    audio_id UUID NOT NULL REFERENCES audio_assessments(audio_id) ON DELETE CASCADE,
    job_type VARCHAR(30) NOT NULL,
    status job_status NOT NULL DEFAULT 'PROCESSING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    UNIQUE (audio_id, job_type)
);

-- Intermediate AI output — teacher reviews before finalization.
-- FIX #4: unique (audio_id, rubric_id) — one staging row per rubric per audio.
CREATE TABLE staging_assessments (
    staging_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    audio_id UUID NOT NULL REFERENCES audio_assessments(audio_id) ON DELETE CASCADE,
    rubric_id UUID NOT NULL REFERENCES knec_rubrics(rubric_id) ON DELETE RESTRICT,
    transcript_snippet TEXT NOT NULL,
    parsed_json_payload JSONB,
    status staging_status NOT NULL DEFAULT 'processing',
    UNIQUE (audio_id, rubric_id)
);

-- Final authoritative record — one score per student per rubric per audio.
-- FIX #5: unique (audio_id, student_id, rubric_id).
CREATE TABLE assessment_records (
    record_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    audio_id UUID NOT NULL REFERENCES audio_assessments(audio_id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(student_id) ON DELETE RESTRICT,
    rubric_id UUID NOT NULL REFERENCES knec_rubrics(rubric_id) ON DELETE RESTRICT,
    score NUMERIC(5,2) NOT NULL,
    UNIQUE (audio_id, student_id, rubric_id)
);

-- AI-generated qualitative feedback, one per assessment record.
CREATE TABLE feedback (
    feedback_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assessment_record_id UUID UNIQUE NOT NULL REFERENCES assessment_records(record_id) ON DELETE CASCADE,
    qualitative_feedback TEXT NOT NULL,
    generated_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Immutable version history — every teacher override creates a new row.
CREATE TABLE assessment_versions (
    version_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assessment_record_id UUID NOT NULL REFERENCES assessment_records(record_id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    json_data JSONB NOT NULL,
    edited_by UUID NOT NULL REFERENCES teachers(teacher_id) ON DELETE RESTRICT,
    edit_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_original BOOLEAN NOT NULL DEFAULT FALSE
);

-- Student appeals — FIX #6 + #7: appeal_status enum, unique (audio_id, student_id).
CREATE TABLE assessment_appeals (
    appeal_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    audio_id UUID NOT NULL REFERENCES audio_assessments(audio_id) ON DELETE RESTRICT,
    student_id UUID NOT NULL REFERENCES students(student_id) ON DELETE RESTRICT,
    reason TEXT NOT NULL,
    status appeal_status NOT NULL DEFAULT 'pending',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_by UUID REFERENCES teachers(teacher_id) ON DELETE SET NULL,
    resolved_at TIMESTAMP,
    UNIQUE (audio_id, student_id)
);

-- ==============================================================================
-- 7. SYSTEM AUDIT
-- ==============================================================================
-- FIX #8: audio_id FK with ON DELETE SET NULL so error logs survive assessment deletion.
CREATE TABLE system_error_logs (
    log_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    error_type VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    related_entity VARCHAR(255),
    audio_id UUID REFERENCES audio_assessments(audio_id) ON DELETE SET NULL
);
