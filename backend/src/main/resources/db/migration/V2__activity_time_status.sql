-- Historical migration marker: activity time/status fields are included in V1 for fresh installs.
-- Existing databases should use docs/sql/activity_time_status_patch.sql and docs/sql/activity_mode_patch.sql.
SELECT 1;
