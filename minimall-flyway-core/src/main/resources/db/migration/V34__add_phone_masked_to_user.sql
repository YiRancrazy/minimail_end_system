-- V34: Add phone_masked column to t_user for fuzzy query support.
-- The phone column stores AES-256-GCM encrypted full phone number.
-- The phone_masked column stores masked version (e.g., 138****8001) for LIKE queries.

ALTER TABLE t_user ADD COLUMN phone_masked VARCHAR(32) DEFAULT NULL COMMENT 'Masked phone for fuzzy query (e.g., 138****8001)';

CREATE INDEX idx_user_phone_masked ON t_user (phone_masked);
