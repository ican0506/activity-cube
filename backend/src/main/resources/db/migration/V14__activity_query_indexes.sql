-- Optimize activity list pagination queries.
-- Keep migration simple for the official V1-V13 schema: replace the old
-- campus/status index with an ordered pagination index, and add a general
-- status/create_time/id index for default list pages.

DROP INDEX idx_activity_campus_status ON activity;

CREATE INDEX idx_activity_status_create
ON activity (`status`, `create_time`, `id`);

CREATE INDEX idx_activity_campus_status_create
ON activity (`campus`, `status`, `create_time`, `id`);
