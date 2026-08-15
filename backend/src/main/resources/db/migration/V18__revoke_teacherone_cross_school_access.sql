-- V18 -- Remove the test backdoor from V12
-- V12 granted teacherone@gmail.com every class and subject across ALL schools
-- so the assessment flow could be tested. That's a cross-school data leak —
-- keep only assignments inside teacherone's own school.

DELETE FROM teacher_class_assignments tca
USING teachers t, classes c
WHERE tca.teacher_id = t.teacher_id
  AND tca.class_id = c.class_id
  AND t.user_id = (SELECT user_id FROM users WHERE email = 'teacherone@gmail.com')
  AND c.school_id <> t.school_id;

DELETE FROM teacher_subject_assignments tsa
USING teachers t, subjects s
WHERE tsa.teacher_id = t.teacher_id
  AND tsa.subject_id = s.subject_id
  AND t.user_id = (SELECT user_id FROM users WHERE email = 'teacherone@gmail.com')
  AND s.school_id <> t.school_id;
