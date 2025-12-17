-- Enable extension for UUID generation (Postgres >=13 may have gen_random_uuid)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE teams (
                       id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                       name text NOT NULL,
                       created_at timestamptz DEFAULT now()
);

CREATE TABLE users (
                       id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                       team_id uuid REFERENCES teams(id),
                       email text UNIQUE NOT NULL,
                       password_hash text,
                       role text DEFAULT 'member',
                       created_at timestamptz DEFAULT now()
);

CREATE TABLE api_keys (
                          id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                          team_id uuid REFERENCES teams(id),
                          key_hash text NOT NULL,
                          created_at timestamptz DEFAULT now()
);

CREATE TABLE workflows (
                           id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                           team_id uuid REFERENCES teams(id),
                           name text NOT NULL,
                           spec jsonb NOT NULL,
                           active boolean DEFAULT false,
                           created_at timestamptz DEFAULT now(),
                           updated_at timestamptz DEFAULT now()
);

CREATE TABLE runs (
                      id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                      workflow_id uuid REFERENCES workflows(id),
                      status text NOT NULL DEFAULT 'queued',   -- queued, running, success, failed, cancelled
                      input jsonb,
                      output jsonb,
                      created_at timestamptz DEFAULT now(),
                      started_at timestamptz,
                      finished_at timestamptz
);

CREATE TABLE run_steps (
                           id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                           run_id uuid REFERENCES runs(id),
                           step_index int,
                           step_id text,
                           name text,
                           status text,
                           input jsonb,
                           output jsonb,
                           log jsonb,
                           started_at timestamptz,
                           finished_at timestamptz
);

-- Incoming events (webhook payloads)
CREATE TABLE incoming_events (
                                 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                 workflow_id uuid REFERENCES workflows(id),
                                 payload jsonb,
                                 idempotency_key text,
                                 received_at timestamptz DEFAULT now()
);
