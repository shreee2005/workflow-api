CREATE TABLE IF NOT EXISTS workflow_states (
    run_id uuid PRIMARY KEY,
    current_step int,
    execution_context text,
    checkpoint_id uuid NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_workflow_states_updated_at
ON workflow_states(updated_at);