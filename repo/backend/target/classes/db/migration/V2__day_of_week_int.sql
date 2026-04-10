-- Align day_of_week with JPA Integer -> INTEGER (Hibernate validates against TINYINT mismatch)
ALTER TABLE schedules MODIFY COLUMN day_of_week INT NULL;
ALTER TABLE locked_periods MODIFY COLUMN day_of_week INT NULL;
