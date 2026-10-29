CREATE TABLE courses (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE tasks
    ADD COLUMN course_id UUID NULL,
    ADD CONSTRAINT fk_tasks_course
        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL;

CREATE INDEX idx_tasks_course_id ON tasks(course_id);
