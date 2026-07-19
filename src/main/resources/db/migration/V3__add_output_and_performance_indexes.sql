ALTER TABLE workflow_run_steps
ADD COLUMN IF NOT EXISTS output text;

CREATE INDEX IF NOT EXISTS idx_workflow_run_steps_run_step
ON workflow_run_steps(run_id, step_index);

CREATE INDEX IF NOT EXISTS idx_workflow_run_steps_run_status
ON workflow_run_steps(run_id, status);

CREATE INDEX IF NOT EXISTS idx_workflow_runs_workflow_started
ON workflow_runs(workflow_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_workflow_runs_status
ON workflow_runs(status);

CREATE INDEX IF NOT EXISTS idx_workflows_active
ON workflows(active);