-- 活动审核、取消报名与操作日志补丁
-- 在 Navicat 中连接 activity_cube 数据库后直接执行。
-- 不删除现有表、不清空数据；兼容 MySQL 5.7+。

USE `activity_cube`;

SET @schema_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'reject_reason') = 0,
  'ALTER TABLE `activity` ADD COLUMN `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT ''活动驳回原因'' AFTER `status`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
   WHERE CONSTRAINT_SCHEMA = @schema_name AND TABLE_NAME = 'activity' AND CONSTRAINT_NAME = 'chk_activity_status') > 0,
  'ALTER TABLE `activity` DROP CHECK `chk_activity_status`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `activity`
SET `status` = 'PUBLISHED'
WHERE `status` NOT IN ('DRAFT', 'PENDING_REVIEW', 'REJECTED', 'PUBLISHED', 'CANCELLED');

ALTER TABLE `activity`
  MODIFY COLUMN `status` VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
  COMMENT '活动工作流状态：DRAFT草稿/PENDING_REVIEW待审核/REJECTED已驳回/PUBLISHED已发布/CANCELLED已取消；已结束由后端按时间计算';

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
   WHERE CONSTRAINT_SCHEMA = @schema_name AND TABLE_NAME = 'activity' AND CONSTRAINT_NAME = 'chk_activity_workflow_status') = 0,
  'ALTER TABLE `activity` ADD CONSTRAINT `chk_activity_workflow_status` CHECK (`status` IN (''DRAFT'', ''PENDING_REVIEW'', ''REJECTED'', ''PUBLISHED'', ''CANCELLED''))',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '操作日志ID',
  `user_id` BIGINT NOT NULL COMMENT '操作人用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作人账号',
  `role` VARCHAR(20) DEFAULT NULL COMMENT '操作人角色',
  `operation` VARCHAR(64) NOT NULL COMMENT '操作类型',
  `target_type` VARCHAR(32) DEFAULT NULL COMMENT '目标类型',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标ID',
  `detail` VARCHAR(1000) DEFAULT NULL COMMENT '操作详情',
  `ip` VARCHAR(64) DEFAULT NULL COMMENT '操作IP地址',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_user` (`user_id`),
  KEY `idx_operation_log_operation` (`operation`),
  KEY `idx_operation_log_create_time` (`create_time`),
  KEY `idx_operation_log_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';
