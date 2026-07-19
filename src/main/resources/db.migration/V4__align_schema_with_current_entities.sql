-- api_keys alignment
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS name text;
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS secret_hash text;
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS revoked boolean NOT NULL DEFAULT false;

UPDATE api_keys
SET secret_hash = key_hash
WHERE secret_hash IS NULL
  AND key_hash IS NOT NULL;

-- users alignment
ALTER TABLE users ADD COLUMN IF NOT EXISTS password text;
ALTER TABLE users ADD COLUMN IF NOT EXISTS name text;
ALTER TABLE users ADD COLUMN IF NOT EXISTS oauth_provider text;
ALTER TABLE users ADD COLUMN IF NOT EXISTS oauth_id text;

UPDATE users
SET password = password_hash
WHERE password IS NULL
  AND password_hash IS NOT NULL;

-- workflows alignment
ALTER TABLE workflows ADD COLUMN IF NOT EXISTS owner_id uuid;
ALTER TABLE workflows ADD COLUMN IF NOT EXISTS active_version_id uuid;
ALTER TABLE workflows ADD COLUMN IF NOT EXISTS active_version_number int;