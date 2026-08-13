-- Align Flyway-generated activity schema with the maintained full SQL.
-- These constraints match the current ActivityService and ActivityStatusUtil
-- business model.

ALTER TABLE activity
ADD CONSTRAINT chk_activity_mode
CHECK (`activity_mode` IN ('online', 'offline', 'hybrid'));

ALTER TABLE activity
ADD CONSTRAINT chk_activity_checkin_mode
CHECK (`checkin_mode` IN ('online', 'qr', 'both'));

ALTER TABLE activity
ADD CONSTRAINT chk_activity_campus
CHECK (`campus` IN ('全校区', '龙子湖校区', '文化路校区', '许昌校区', '线上'));

ALTER TABLE activity
ADD CONSTRAINT chk_activity_workflow_status
CHECK (`status` IN ('DRAFT', 'PENDING_REVIEW', 'REJECTED', 'PUBLISHED', 'CANCELLED'));

ALTER TABLE activity
ADD CONSTRAINT chk_activity_max_participants
CHECK (`max_participants` IS NULL OR `max_participants` > 0);

ALTER TABLE activity
ADD CONSTRAINT chk_activity_time
CHECK (`end_time` > `start_time`);

ALTER TABLE activity
ADD CONSTRAINT chk_activity_register_time
CHECK (`register_end_time` > `register_start_time`);

ALTER TABLE activity
ADD CONSTRAINT chk_activity_checkin_time
CHECK (`checkin_end_time` > `checkin_start_time`);
