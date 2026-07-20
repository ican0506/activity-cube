SET @schema_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'activity_category') = 0,
  'ALTER TABLE `activity` ADD COLUMN `activity_category` VARCHAR(50) NOT NULL DEFAULT ''其他'' COMMENT ''活动类型'' AFTER `activity_mode`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'reward_enabled') = 0,
  'ALTER TABLE `activity` ADD COLUMN `reward_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否设置奖励'' AFTER `allow_cross_campus`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'reward_type') = 0,
  'ALTER TABLE `activity` ADD COLUMN `reward_type` VARCHAR(30) NOT NULL DEFAULT ''无'' COMMENT ''奖励类型'' AFTER `reward_enabled`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'reward_hours') = 0,
  'ALTER TABLE `activity` ADD COLUMN `reward_hours` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT ''课外学时数量'' AFTER `reward_type`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'reward_points') = 0,
  'ALTER TABLE `activity` ADD COLUMN `reward_points` INT NOT NULL DEFAULT 0 COMMENT ''积分数量'' AFTER `reward_hours`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'reward_description') = 0,
  'ALTER TABLE `activity` ADD COLUMN `reward_description` VARCHAR(500) DEFAULT NULL COMMENT ''奖励说明'' AFTER `reward_points`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `student_activity_reward` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '奖励记录ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `student_id` BIGINT NOT NULL COMMENT '学生用户ID',
  `student_no` VARCHAR(50) DEFAULT NULL COMMENT '学号',
  `activity_category` VARCHAR(50) DEFAULT NULL COMMENT '活动类型',
  `reward_type` VARCHAR(30) NOT NULL COMMENT '奖励类型',
  `reward_hours` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '课外学时数量',
  `reward_points` INT NOT NULL DEFAULT 0 COMMENT '积分数量',
  `reward_description` VARCHAR(500) DEFAULT NULL COMMENT '奖励说明',
  `issued_by` BIGINT NOT NULL COMMENT '发放操作人ID',
  `issued_by_name` VARCHAR(100) DEFAULT NULL COMMENT '发放操作人姓名',
  `issued_time` DATETIME NOT NULL COMMENT '发放时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'issued' COMMENT '发放状态',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_activity_reward` (`activity_id`, `student_id`),
  KEY `idx_reward_student` (`student_id`),
  KEY `idx_reward_activity` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生活动奖励记录表';
