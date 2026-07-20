-- 活动魔方：唯一约束补丁
-- 执行前请先运行 docs/sql/duplicate_check.sql 检查重复数据。
-- 如果检查结果不为空，请先清理重复数据后再执行本文件。

USE `activity_cube`;

SET @schema_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND INDEX_NAME = 'uk_user_username') = 0,
  'ALTER TABLE `user` ADD UNIQUE KEY `uk_user_username` (`username`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND INDEX_NAME = 'uk_user_student_no') = 0,
  'ALTER TABLE `user` ADD UNIQUE KEY `uk_user_student_no` (`student_no`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND INDEX_NAME = 'uk_user_work_no') = 0,
  'ALTER TABLE `user` ADD UNIQUE KEY `uk_user_work_no` (`work_no`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'registration' AND INDEX_NAME = 'uk_registration_activity_user') = 0,
  'ALTER TABLE `registration` ADD UNIQUE KEY `uk_registration_activity_user` (`activity_id`, `user_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'registration' AND INDEX_NAME = 'uk_registration_activity_student_no') = 0,
  'ALTER TABLE `registration` ADD UNIQUE KEY `uk_registration_activity_student_no` (`activity_id`, `student_no`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'checkin' AND INDEX_NAME = 'uk_checkin_activity_user') = 0,
  'ALTER TABLE `checkin` ADD UNIQUE KEY `uk_checkin_activity_user` (`activity_id`, `user_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
