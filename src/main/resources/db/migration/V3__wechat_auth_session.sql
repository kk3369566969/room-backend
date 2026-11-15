CREATE TABLE IF NOT EXISTS auth_session (
  id BINARY(16) PRIMARY KEY,
  token_hash CHAR(64) NOT NULL UNIQUE,
  external_id VARCHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  revoked_at DATETIME(6),
  INDEX idx_auth_session_external (external_id),
  INDEX idx_auth_session_expiry (expires_at)
);
