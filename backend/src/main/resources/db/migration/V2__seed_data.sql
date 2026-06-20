-- ==============================================================================
-- VoiceAssess V2 — Seed data for development
-- Inserts sample records in FK-safe order.
-- Passwords are bcrypt hashes of "password123" (placeholder — real auth later).
-- UUIDs use hex chars only (0-9, a-f).
-- ==============================================================================

-- ── 1. School ──────────────────────────────────────────────────────────────
INSERT INTO schools (school_id, school_name, knec_code)
VALUES ('a1000000-0000-0000-0000-000000000001', 'Strathmore Primary', 'ST-001');

-- ── 2. Users (four roles) ─────────────────────────────────────────────────
INSERT INTO users (user_id, email, password_hash, role)
VALUES
    ('b1000000-0000-0000-0000-000000000001', 'admin@strathmore.ac.ke',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN'),
    ('b1000000-0000-0000-0000-000000000002', 'teacher@strathmore.ac.ke',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'TEACHER'),
    ('b1000000-0000-0000-0000-000000000003', 'parent@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'PARENT'),
    ('b1000000-0000-0000-0000-000000000004', 'student@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT');

-- ── 3. Profiles (link to users + school) ──────────────────────────────────
INSERT INTO administrators (admin_id, user_id, school_id, full_name, registration_number, contact_email, capability_array)
VALUES ('c1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001',
        'a1000000-0000-0000-0000-000000000001', 'Alice Admin', 'REG-ADM-001',
        'alice.admin@strathmore.ac.ke', '["CREATE_TEACHER","VIEW_REPORTS","MANAGE_SCHOOL"]');

INSERT INTO teachers (teacher_id, user_id, school_id, full_name)
VALUES ('c2000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000002',
        'a1000000-0000-0000-0000-000000000001', 'Bob Teacher');

INSERT INTO parents (parent_id, user_id, full_name, phone_number)
VALUES ('c3000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000003',
        'Carol Parent', '+254712345678');

INSERT INTO students (student_id, user_id, school_id, full_name, date_of_birth)
VALUES ('c4000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000004',
        'a1000000-0000-0000-0000-000000000001', 'David Student', '2015-03-15');

-- ── 4. Parent-Student link (M:N junction) ─────────────────────────────────
INSERT INTO student_parents (student_id, parent_id)
VALUES ('c4000000-0000-0000-0000-000000000001', 'c3000000-0000-0000-0000-000000000001');

-- ── 5. Academic term ──────────────────────────────────────────────────────
INSERT INTO academic_terms (term_id, school_id, term_name, start_date, end_date, status)
VALUES ('d1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
        'Term 1 2026', '2026-01-05', '2026-03-28', 'active');

-- ── 6. Class ──────────────────────────────────────────────────────────────
INSERT INTO classes (class_id, school_id, grade_level, stream_name)
VALUES ('e1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
        4, 'East');

-- ── 7. Subjects ───────────────────────────────────────────────────────────
INSERT INTO subjects (subject_id, subject_name, grade_level)
VALUES
    ('f1000000-0000-0000-0000-000000000001', 'Mathematics', 4),
    ('f1000000-0000-0000-0000-000000000002', 'English', 4);

-- ── 8. KNEC Rubrics ───────────────────────────────────────────────────────
INSERT INTO knec_rubrics (rubric_id, subject_id, competency_desc, strand, sub_strand, rating_scale)
VALUES
    ('a2010000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000001',
     'Able to count numbers 1 to 100 fluently', 'Numbers', 'Whole Numbers',
     'Below / Approaching / Meeting / Exceeding'),
    ('a2020000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002',
     'Can read a short passage aloud with correct pronunciation', 'Reading', 'Oral Reading',
     'Below / Approaching / Meeting / Exceeding');

-- ── 9. Enrollments & Assignments ──────────────────────────────────────────
INSERT INTO student_enrollments (student_id, class_id)
VALUES ('c4000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001');

INSERT INTO teacher_class_assignments (teacher_id, class_id)
VALUES ('c2000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001');

INSERT INTO teacher_subject_assignments (teacher_id, subject_id)
VALUES
    ('c2000000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000001'),
    ('c2000000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002');
