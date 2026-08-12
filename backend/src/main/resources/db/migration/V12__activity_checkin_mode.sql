SET @schema_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'checkin_mode') = 0,
  'ALTER TABLE `activity` ADD COLUMN `checkin_mode` VARCHAR(20) NOT NULL DEFAULT ''qr'' COMMENT ''签到方式：online线上签到/qr现场扫码签到/both两种都支持'' AFTER `activity_mode`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `activity`
SET `checkin_mode` = CASE
  WHEN `activity_mode` = 'online' THEN 'online'
  WHEN `activity_mode` = 'hybrid' THEN 'online'
  ELSE 'qr'
END
WHERE `checkin_mode` IS NULL OR `checkin_mode` = '';
