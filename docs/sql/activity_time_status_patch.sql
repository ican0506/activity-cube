-- 活动魔方：活动时间与自动状态补丁
-- 执行位置：Navicat 连接 activity_cube 数据库后执行
-- 说明：
-- 1. 现有表中的 register_start_time / register_end_time 就是报名开始/结束时间字段。
-- 2. 本补丁新增 checkin_start_time / checkin_end_time。
-- 3. status 字段改为只保存人工状态：DRAFT 草稿 / PUBLISHED 已发布 / CANCELLED 已取消。
-- 4. 页面展示的“未开始、报名中、待开始、进行中、已结束”由后端按时间自动计算。

USE `activity_cube`;

-- ALTER TABLE `activity` DROP CHECK `chk_activity_status`;

ALTER TABLE `activity`
  ADD COLUMN `checkin_start_time` DATETIME NULL COMMENT '签到开始时间' AFTER `register_end_time`,
  ADD COLUMN `checkin_end_time` DATETIME NULL COMMENT '签到结束时间' AFTER `checkin_start_time`;

UPDATE `activity`
SET
  `checkin_start_time` = COALESCE(`checkin_start_time`, `start_time`),
  `checkin_end_time` = COALESCE(`checkin_end_time`, `end_time`);

UPDATE `activity`
SET `status` = 'PUBLISHED'
WHERE `status` NOT IN ('DRAFT', 'CANCELLED', 'PUBLISHED');

ALTER TABLE `activity`
  MODIFY COLUMN `register_start_time` DATETIME NOT NULL COMMENT '报名开始时间',
  MODIFY COLUMN `register_end_time` DATETIME NOT NULL COMMENT '报名结束时间',
  MODIFY COLUMN `checkin_start_time` DATETIME NOT NULL COMMENT '签到开始时间',
  MODIFY COLUMN `checkin_end_time` DATETIME NOT NULL COMMENT '签到结束时间',
  MODIFY COLUMN `status` VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED' COMMENT '人工状态：DRAFT草稿/PUBLISHED已发布/CANCELLED已取消，展示状态由后端按时间自动计算',
  ADD CONSTRAINT `chk_activity_status` CHECK (`status` IN ('DRAFT', 'PUBLISHED', 'CANCELLED')),
  ADD CONSTRAINT `chk_activity_checkin_time` CHECK (`checkin_end_time` > `checkin_start_time`);

-- 给已有测试活动填充合理时间窗口，可按需要自行调整。
UPDATE `activity`
SET
  `register_start_time` = COALESCE(`register_start_time`, DATE_SUB(`start_time`, INTERVAL 5 DAY)),
  `register_end_time` = COALESCE(`register_end_time`, DATE_SUB(`start_time`, INTERVAL 2 HOUR)),
  `checkin_start_time` = COALESCE(`checkin_start_time`, DATE_SUB(`start_time`, INTERVAL 15 MINUTE)),
  `checkin_end_time` = COALESCE(`checkin_end_time`, DATE_ADD(`end_time`, INTERVAL 15 MINUTE))
WHERE `id` IS NOT NULL;
