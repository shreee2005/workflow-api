CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS teams (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    owner_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id uuid REFERENCES teams(id),
    email text UNIQUE NOT NULL,
    password_hash text,
    role text DEFAULT 'member',
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS team_members (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id uuid NOT NULL REFERENCES teams(id),
    email text NOT NULL,
    user_id uuid REFERENCES users(id),
    status text NOT NULL DEFAULT 'INVITED',
    invited_at timestamptz NOT NULL DEFAULT now(),
    accepted_at timestamptz,
    CONSTRAINT unique_team_email UNIQUE (team_id, email)
);

CREATE TABLE IF NOT EXISTS api_keys (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id uuid REFERENCES teams(id),
    key_hash text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS workflows (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id uuid REFERENCES teams(id),
    name text NOT NULL,
    spec jsonb NOT NULL,
    active boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS workflow_templates (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    category text NOT NULL,
    description text NOT NULL,
    spec jsonb NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS workflow_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id uuid NOT NULL REFERENCES workflows(id),
    version_number int NOT NULL,
    spec jsonb NOT NULL,
    change_note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_workflow_version_number UNIQUE (workflow_id, version_number)
);

CREATE TABLE IF NOT EXISTS workflow_runs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id uuid REFERENCES workflows(id),
    workflow_version_id uuid REFERENCES workflow_versions(id),
    incoming_event_id uuid,
    status text NOT NULL DEFAULT 'QUEUED',
    error_message text,
    attempt int NOT NULL DEFAULT 0,
    max_attempts int NOT NULL DEFAULT 3,
    dead_lettered boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    finished_at timestamptz
);

CREATE TABLE IF NOT EXISTS workflow_run_steps (
    id uuid PRIMARY KEY,
    run_id uuid NOT NULL REFERENCES workflow_runs(id),
    step_index int NOT NULL,
    step_type text NOT NULL,
    status text NOT NULL,
    logs text,
    error_message text,
    started_at timestamptz,
    finished_at timestamptz
);

CREATE TABLE IF NOT EXISTS incoming_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id uuid REFERENCES workflows(id),
    payload jsonb,
    idempotency_key text,
    received_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS workflow_wait_states (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id uuid NOT NULL UNIQUE REFERENCES workflow_runs(id),
    workflow_id uuid NOT NULL REFERENCES workflows(id),
    workflow_version_id uuid NOT NULL REFERENCES workflow_versions(id),
    step_index int NOT NULL,
    correlation_id text NOT NULL UNIQUE,
    status text NOT NULL,
    callback_payload text,
    created_at timestamptz NOT NULL DEFAULT now(),
    resumed_at timestamptz
);

CREATE TABLE IF NOT EXISTS plugins (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_key text UNIQUE NOT NULL,
    name text NOT NULL,
    description text,
    category text NOT NULL,
    icon text,
    config_schema jsonb,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);