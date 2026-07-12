-- V17: add school_id to parents table so they are scoped per school
-- Parents were the only profile table without school_id, making them global across all schools

ALTER TABLE parents ADD COLUMN school_id UUID;

-- backfill existing parents to the seed school
UPDATE parents SET school_id = 'a1000000-0000-0000-0000-000000000001'
WHERE school_id IS NULL;

ALTER TABLE parents ALTER COLUMN school_id SET NOT NULL;

ALTER TABLE parents ADD CONSTRAINT fk_parents_school
    FOREIGN KEY (school_id) REFERENCES schools(school_id) ON DELETE RESTRICT;
