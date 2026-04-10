-- Align difficulty_level with JPA Integer -> INTEGER (Hibernate validates against TINYINT mismatch)
ALTER TABLE questions MODIFY COLUMN difficulty_level INT NOT NULL CHECK (difficulty_level BETWEEN 1 AND 5);
ALTER TABLE quiz_rules MODIFY COLUMN difficulty_level INT NULL;
