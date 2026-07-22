-- 活动魔方：活动签到方式字段补丁
-- 适用于已有 activity_cube 数据库升级；不会删除表，不会清空数据。
USE activity_cube;

SET @column_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity'
    AND COLUMN_NAME = 'checkin_mode'
);

SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE activity ADD COLUMN checkin_mode VARCHAR(20) DEFAULT ''online'' COMMENT ''签到方式：online线上签到，qr现场扫码签到，both两种都支持'' AFTER activity_mode',
  'SELECT ''checkin_mode already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE activity
SET checkin_mode = CASE
  WHEN activity_mode = 'offline' THEN 'qr'
  WHEN activity_mode = 'online' THEN 'online'
  WHEN activity_mode = 'hybrid' THEN 'online'
  ELSE 'online'
END
WHERE checkin_mode IS NULL OR checkin_mode = '';
