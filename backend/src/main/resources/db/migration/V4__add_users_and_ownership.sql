-- Slice 6 introduces real ownership. Pre-auth trial data has no legitimate owner,
-- so the private-development dataset is intentionally reset once rather than
-- assigning it to a hidden/fake account.
DELETE FROM tasks;
DELETE FROM courses;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE courses
    ADD COLUMN user_id UUID NOT NULL,
    ADD CONSTRAINT fk_courses_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE tasks
    ADD COLUMN user_id UUID NOT NULL,
    ADD CONSTRAINT fk_tasks_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_courses_user_name ON courses(user_id, name, id);
CREATE INDEX idx_tasks_user_status_due ON tasks(user_id, status, due_at, id);
CREATE INDEX idx_tasks_user_course_status ON tasks(user_id, course_id, status);
