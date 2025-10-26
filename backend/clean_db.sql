-- Connect to database: psql -h localhost -U sora -d sora
-- Run this script to clean the database

-- Drop all tables
DROP TABLE IF EXISTS notification CASCADE;
DROP TABLE IF EXISTS comment CASCADE;
DROP TABLE IF EXISTS like_post CASCADE;
DROP TABLE IF EXISTS follow CASCADE;
DROP TABLE IF EXISTS post_media CASCADE;
DROP TABLE IF EXISTS post CASCADE;
DROP TABLE IF EXISTS trip_participant CASCADE;
DROP TABLE IF EXISTS trip_invitation CASCADE;
DROP TABLE IF EXISTS trip CASCADE;
DROP TABLE IF EXISTS collection CASCADE;
DROP TABLE IF EXISTS city CASCADE;
DROP TABLE IF EXISTS country CASCADE;
DROP TABLE IF EXISTS user_account CASCADE;

-- Clean Flyway schema history
DROP TABLE IF EXISTS flyway_schema_history CASCADE;

-- Verify tables are gone
\dt