-- 管理员用户管理模块补丁
-- 使用方式：在 Navicat 中连接 activity_cube 数据库后直接执行本文件。
-- 说明：本补丁只补字段、迁移普通学生角色命名和更新约束，不删除表、不清空数据。

USE `activity_cube`;

SET @schema_name = 'activity_cube';

SET @sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name
     AND TABLE_NAME = 'user'
     AND COLUMN_NAME = 'status') = 0,
  'ALTER TABLE `user` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT ''账号状态：1启用，0禁用''',
  'SELECT ''status 字段已存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `user`
SET `status` = 1
WHERE `status` IS NULL;

SET @sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.TABLE_CONSTRAINTS
   WHERE CONSTRAINT_SCHEMA = @schema_name
     AND TABLE_NAME = 'user'
     AND CONSTRAINT_NAME = 'chk_user_role') > 0,
  'ALTER TABLE `user` DROP CHECK `chk_user_role`',
  'SELECT ''chk_user_role 约束不存在'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `user`
SET `role` = 'student'
WHERE `role` = 'user';

ALTER TABLE `user`
ADD CONSTRAINT `chk_user_role`
CHECK (`role` IN ('student', 'organizer', 'admin'));
