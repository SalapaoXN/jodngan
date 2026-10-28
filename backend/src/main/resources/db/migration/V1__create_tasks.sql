CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL,
    completed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_tasks_status CHECK (status IN ('ACTIVE', 'COMPLETED')),
    CONSTRAINT ck_tasks_completion CHECK (
        (status = 'ACTIVE' AND completed_at IS NULL)
        OR (status = 'COMPLETED' AND completed_at IS NOT NULL)
    )
);
