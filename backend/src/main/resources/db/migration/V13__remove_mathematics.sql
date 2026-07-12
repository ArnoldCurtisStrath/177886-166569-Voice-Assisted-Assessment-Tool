-- V13 -- Remove Mathematics subject (not in our 3-subject scope)
-- Delete FK-safe: child tables first, then parent.

DELETE FROM teacher_subject_assignments
WHERE subject_id = 'f1000000-0000-0000-0000-000000000001';

DELETE FROM knec_rubrics
WHERE subject_id = 'f1000000-0000-0000-0000-000000000001';

DELETE FROM subjects
WHERE subject_id = 'f1000000-0000-0000-0000-000000000001';
