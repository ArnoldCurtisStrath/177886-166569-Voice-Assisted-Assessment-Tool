-- V12 -- Give teacherone@gmail.com access to ALL classes and subjects
-- This teacher was created via the admin API but had zero assignments.
-- Now they can test the full assessment flow.

INSERT INTO teacher_class_assignments (teacher_id, class_id)
SELECT t.teacher_id, c.class_id
FROM teachers t
JOIN users u ON t.user_id = u.user_id
CROSS JOIN classes c
WHERE u.email = 'teacherone@gmail.com'
ON CONFLICT (teacher_id, class_id) DO NOTHING;

INSERT INTO teacher_subject_assignments (teacher_id, subject_id)
SELECT t.teacher_id, s.subject_id
FROM teachers t
JOIN users u ON t.user_id = u.user_id
CROSS JOIN subjects s
WHERE u.email = 'teacherone@gmail.com'
ON CONFLICT (teacher_id, subject_id) DO NOTHING;
