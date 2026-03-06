-- V3: Add CODE_POOL table for pre-generated short codes
-- This table stores a pool of pre-generated short codes that can be assigned to users

CREATE TABLE IF NOT EXISTS CODE_POOL (
    ID BIGSERIAL PRIMARY KEY,
    CODE VARCHAR(20) NOT NULL UNIQUE,
    IS_USED BOOLEAN NOT NULL DEFAULT FALSE,
    ASSIGNED_USERID BIGINT,
    CREATEDAT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for finding available codes quickly
CREATE INDEX IDX_CODE_POOL_IS_USED ON CODE_POOL(IS_USED);

-- Index for counting available codes
CREATE INDEX IDX_CODE_POOL_COUNT ON CODE_POOL(IS_USED) WHERE IS_USED = FALSE;

-- Index for finding codes by code string
CREATE INDEX IDX_CODE_POOL_CODE ON CODE_POOL(CODE);
