-- V2: Add Expiry Date Indexes and Performance Improvements
-- Adds indexes for expiry date queries and user URL lookups

-- Add index on expiresAt for efficient expiration checks
CREATE INDEX IF NOT EXISTS idx_urls_expiresat ON URLS(EXPIRESAT);

-- Add composite index for finding active URLs by user with expiry check
CREATE INDEX IF NOT EXISTS idx_urls_userid_isactive_expiresat ON URLS(USERID, ISACTIVE, EXPIRESAT);

-- Add index on EMAIL column in USERS table for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON USERS(EMAIL);

-- Add comments to document the schema
COMMENT ON TABLE USERS IS 'Stores user accounts for the URL shortener';
COMMENT ON TABLE URLS IS 'Stores shortened URLs with ownership and expiration';
COMMENT ON TABLE CLICKEVENTS IS 'Tracks clicks on shortened URLs for analytics';
COMMENT ON TABLE URL_PREFERENCE IS 'User preferences for URL shortening strategies';
