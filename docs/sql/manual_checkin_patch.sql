-- 人工补签功能数据库补丁
-- 在 Navicat 中连接 activity_cube 数据库后执行。
-- 不删除原表、不清空数据；兼容 MySQL 5.7+，可重复执行。

USE `activity_cube`;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `checkin` ADD COLUMN `checkin_type` VARCHAR(16) NOT NULL DEFAULT ''qr'' COMMENT ''签到方式：qr扫码、online线上、manual人工补签'' AFTER `campus`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'checkin' AND COLUMN_NAME = 'checkin_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `checkin` ADD COLUMN `operator_id` BIGINT DEFAULT NULL COMMENT ''人工补签操作人ID'' AFTER `checkin_type`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'checkin' AND COLUMN_NAME = 'operator_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `checkin` ADD COLUMN `operator_name` VARCHAR(50) DEFAULT NULL COMMENT ''人工补签操作人姓名'' AFTER `operator_id`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'checkin' AND COLUMN_NAME = 'operator_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `checkin` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT ''人工补签原因'' AFTER `operator_name`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'checkin' AND COLUMN_NAME = 'remark'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `checkin`
SET `checkin_type` = 'qr'
WHERE `checkin_type` IS NULL OR `checkin_type` = '';
