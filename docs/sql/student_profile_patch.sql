-- 学生个人中心字段补丁
-- 在 Navicat 中连接 activity_cube 数据库后执行。
-- 不删除原表、不清空数据；兼容 MySQL 5.7+，可重复执行。

USE `activity_cube`;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `user` ADD COLUMN `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT ''头像URL'' AFTER `real_name`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'avatar_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `user` ADD COLUMN `bio` VARCHAR(500) DEFAULT NULL COMMENT ''个人简介'' AFTER `phone`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'bio'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
