ALTER TABLE tasks
    ADD COLUMN estimated_minutes INTEGER NULL,
    ADD COLUMN importance VARCHAR(16) NULL,
    ADD COLUMN notes TEXT NULL;

ALTER TABLE tasks
    ADD CONSTRAINT ck_tasks_estimated_minutes
        CHECK (estimated_minutes IS NULL OR estimated_minutes > 0),
    ADD CONSTRAINT ck_tasks_importance
        CHECK (importance IS NULL OR importance IN ('LOW', 'NORMAL', 'HIGH'));
