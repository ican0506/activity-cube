-- 活动形式与线下签到码补丁
-- 使用方式：在 Navicat 中连接 activity_cube 数据库后直接执行本文件。
-- 说明：本补丁只新增字段并补默认值，不删除表、不清空数据。

USE `activity_cube`;

SET @schema_name = 'activity_cube';

SET @sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name
     AND TABLE_NAME = 'activity'
     AND COLUMN_NAME = 'activity_mode') = 0,
  'ALTER TABLE `activity` ADD COLUMN `activity_mode` VARCHAR(20) NOT NULL DEFAULT ''offline'' COMMENT ''活动形式：online线上活动，offline线下活动''',
  'SELECT ''activity_mode 字段已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name
     AND TABLE_NAME = 'activity'
     AND COLUMN_NAME = 'checkin_code') = 0,
  'ALTER TABLE `activity` ADD COLUMN `checkin_code` VARCHAR(64) DEFAULT NULL COMMENT ''签到码：线下活动现场扫码签到校验使用'' AFTER `activity_mode`',
  'SELECT ''checkin_code 字段已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `activity`
SET `activity_mode` = 'offline'
WHERE `activity_mode` IS NULL OR `activity_mode` = '';

UPDATE `activity`
SET `checkin_code` = REPLACE(UUID(), '-', '')
WHERE `checkin_code` IS NULL OR `checkin_code` = '';
