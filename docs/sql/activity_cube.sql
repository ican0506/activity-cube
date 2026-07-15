-- 活动魔方：校园活动组织一站式轻工具平台
-- MySQL 初始化脚本
-- 可直接复制到 Navicat 执行

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `activity_cube`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `activity_cube`;

DROP TABLE IF EXISTS `feedback`;
DROP TABLE IF EXISTS `checkin`;
DROP TABLE IF EXISTS `registration`;
DROP TABLE IF EXISTS `activity`;
DROP TABLE IF EXISTS `user`;

-- 用户表：学生、活动负责人、管理员
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
  `password` VARCHAR(100) NOT NULL COMMENT '登录密码，MVP阶段可明文，正式环境必须加密',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `student_no` VARCHAR(50) DEFAULT NULL COMMENT '学号或工号',
  `role` VARCHAR(20) NOT NULL COMMENT '用户角色：user学生/organizer活动负责人/admin管理员',
  `campus` VARCHAR(50) DEFAULT NULL COMMENT '所在校区：龙子湖校区/文化路校区/许昌校区',
  `college` VARCHAR(100) DEFAULT NULL COMMENT '学院或部门',
  `major_class` VARCHAR(100) DEFAULT NULL COMMENT '专业班级',
  `phone` VARCHAR(30) DEFAULT NULL COMMENT '手机号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1启用，0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_role` (`role`),
  KEY `idx_user_campus` (`campus`),
  CONSTRAINT `chk_user_role` CHECK (`role` IN ('user', 'organizer', 'admin')),
  CONSTRAINT `chk_user_campus` CHECK (`campus` IS NULL OR `campus` IN ('龙子湖校区', '文化路校区', '许昌校区'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 活动表：活动负责人创建和管理的活动
CREATE TABLE `activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `title` VARCHAR(100) NOT NULL COMMENT '活动名称',
  `description` TEXT NOT NULL COMMENT '活动介绍',
  `campus` VARCHAR(50) NOT NULL COMMENT '活动校区：全校区/龙子湖校区/文化路校区/许昌校区/线上',
  `location` VARCHAR(200) NOT NULL COMMENT '活动地点',
  `start_time` DATETIME NOT NULL COMMENT '活动开始时间',
  `end_time` DATETIME NOT NULL COMMENT '活动结束时间',
  `register_start_time` DATETIME NOT NULL COMMENT '报名开始时间',
  `register_end_time` DATETIME NOT NULL COMMENT '报名结束时间',
  `max_participants` INT DEFAULT NULL COMMENT '最大报名人数，空表示不限人数',
  `allow_cross_campus` TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许跨校区报名：1允许，0不允许',
  `status` VARCHAR(30) NOT NULL DEFAULT 'REGISTERING' COMMENT '活动状态：DRAFT草稿/REGISTERING报名中/ONGOING进行中/ENDED已结束/CANCELLED已取消',
  `creator_id` BIGINT NOT NULL COMMENT '创建人用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_creator` (`creator_id`),
  KEY `idx_activity_campus_status` (`campus`, `status`),
  KEY `idx_activity_start_time` (`start_time`),
  CONSTRAINT `fk_activity_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_activity_campus` CHECK (`campus` IN ('全校区', '龙子湖校区', '文化路校区', '许昌校区', '线上')),
  CONSTRAINT `chk_activity_status` CHECK (`status` IN ('DRAFT', 'REGISTERING', 'ONGOING', 'ENDED', 'CANCELLED')),
  CONSTRAINT `chk_activity_max_participants` CHECK (`max_participants` IS NULL OR `max_participants` > 0),
  CONSTRAINT `chk_activity_time` CHECK (`end_time` > `start_time`),
  CONSTRAINT `chk_activity_register_time` CHECK (`register_end_time` > `register_start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动表';

-- 报名表：学生报名活动记录
CREATE TABLE `registration` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报名记录ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '报名用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '报名姓名',
  `student_no` VARCHAR(50) NOT NULL COMMENT '学号',
  `college` VARCHAR(100) NOT NULL COMMENT '学院',
  `major_class` VARCHAR(100) NOT NULL COMMENT '专业班级',
  `phone` VARCHAR(30) DEFAULT NULL COMMENT '手机号',
  `campus` VARCHAR(50) NOT NULL COMMENT '学生所在校区：龙子湖校区/文化路校区/许昌校区',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '报名备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_registration_activity_user` (`activity_id`, `user_id`),
  KEY `idx_registration_activity` (`activity_id`),
  KEY `idx_registration_user` (`user_id`),
  KEY `idx_registration_campus` (`campus`),
  CONSTRAINT `fk_registration_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_registration_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_registration_campus` CHECK (`campus` IN ('龙子湖校区', '文化路校区', '许昌校区'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报名表';

-- 签到表：学生签到活动记录
CREATE TABLE `checkin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '签到记录ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '签到用户ID',
  `registration_id` BIGINT NOT NULL COMMENT '报名记录ID',
  `checkin_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  `campus` VARCHAR(50) NOT NULL COMMENT '签到用户所在校区',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_checkin_activity_user` (`activity_id`, `user_id`),
  KEY `idx_checkin_activity` (`activity_id`),
  KEY `idx_checkin_user` (`user_id`),
  KEY `idx_checkin_registration` (`registration_id`),
  KEY `idx_checkin_campus` (`campus`),
  CONSTRAINT `fk_checkin_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_checkin_registration` FOREIGN KEY (`registration_id`) REFERENCES `registration` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_checkin_campus` CHECK (`campus` IN ('龙子湖校区', '文化路校区', '许昌校区'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到表';

-- 反馈表：学生对活动提交的反馈
CREATE TABLE `feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈记录ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '反馈用户ID',
  `score` INT NOT NULL COMMENT '满意度评分：1-5分',
  `content` VARCHAR(1000) DEFAULT NULL COMMENT '活动体验',
  `suggestion` VARCHAR(1000) DEFAULT NULL COMMENT '改进建议',
  `anonymous` TINYINT NOT NULL DEFAULT 0 COMMENT '是否匿名：1匿名，0实名',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_feedback_activity_user` (`activity_id`, `user_id`),
  KEY `idx_feedback_activity` (`activity_id`),
  KEY `idx_feedback_user` (`user_id`),
  CONSTRAINT `fk_feedback_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_feedback_score` CHECK (`score` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈表';

-- 测试用户
INSERT INTO `user`
(`id`, `username`, `password`, `real_name`, `student_no`, `role`, `campus`, `college`, `major_class`, `phone`, `status`)
VALUES
(1, 'admin', '123456', '系统管理员', 'A0001', 'admin', '龙子湖校区', '信息化办公室', '系统管理', '13800000000', 1),
(2, 'organizer001', '123456', '活动负责人', 'T2024001', 'organizer', '龙子湖校区', '校团委', '活动管理', '13800000001', 1),
(3, 'student001', '123456', '张三', '2024001', 'user', '龙子湖校区', '信息工程学院', '软件工程2401', '13800000002', 1),
(4, 'student002', '123456', '李四', '2024002', 'user', '文化路校区', '文法学院', '汉语言2401', '13800000003', 1),
(5, 'student003', '123456', '王五', '2024003', 'user', '许昌校区', '商学院', '工商管理2401', '13800000004', 1);

-- 测试活动
INSERT INTO `activity`
(`id`, `title`, `description`, `campus`, `location`, `start_time`, `end_time`, `register_start_time`, `register_end_time`, `max_participants`, `allow_cross_campus`, `status`, `creator_id`)
VALUES
(1, '龙子湖校区校园摄影分享会', '面向龙子湖校区学生的摄影经验分享、作品展示和现场互动活动。', '龙子湖校区', '龙子湖校区大学生活动中心201', '2026-07-20 14:00:00', '2026-07-20 16:00:00', '2026-07-15 08:00:00', '2026-07-20 12:00:00', 120, 0, 'REGISTERING', 2),
(2, '全校区志愿服务动员会', '面向三个校区学生的志愿服务说明会，支持跨校区报名。', '全校区', '线上直播+各校区分会场', '2026-07-22 19:00:00', '2026-07-22 20:30:00', '2026-07-15 08:00:00', '2026-07-22 18:00:00', 300, 1, 'REGISTERING', 2),
(3, '线上AI工具体验课', '介绍常用AI学习与实践工具，活动全程线上进行。', '线上', '腾讯会议', '2026-07-25 19:00:00', '2026-07-25 20:30:00', '2026-07-15 08:00:00', '2026-07-25 18:00:00', 500, 1, 'REGISTERING', 2);

-- 测试报名
INSERT INTO `registration`
(`id`, `activity_id`, `user_id`, `name`, `student_no`, `college`, `major_class`, `phone`, `campus`, `remark`)
VALUES
(1, 1, 3, '张三', '2024001', '信息工程学院', '软件工程2401', '13800000002', '龙子湖校区', '喜欢摄影'),
(2, 2, 3, '张三', '2024001', '信息工程学院', '软件工程2401', '13800000002', '龙子湖校区', NULL),
(3, 2, 4, '李四', '2024002', '文法学院', '汉语言2401', '13800000003', '文化路校区', NULL),
(4, 2, 5, '王五', '2024003', '商学院', '工商管理2401', '13800000004', '许昌校区', NULL),
(5, 3, 4, '李四', '2024002', '文法学院', '汉语言2401', '13800000003', '文化路校区', '线上参与');

-- 测试签到
INSERT INTO `checkin`
(`id`, `activity_id`, `user_id`, `registration_id`, `checkin_time`, `campus`)
VALUES
(1, 1, 3, 1, '2026-07-20 13:45:00', '龙子湖校区'),
(2, 2, 3, 2, '2026-07-22 18:55:00', '龙子湖校区'),
(3, 2, 4, 3, '2026-07-22 18:57:00', '文化路校区');

-- 测试反馈
INSERT INTO `feedback`
(`id`, `activity_id`, `user_id`, `score`, `content`, `suggestion`, `anonymous`)
VALUES
(1, 1, 3, 5, '活动组织清晰，签到很方便。', '希望后续增加活动照片回顾。', 0),
(2, 2, 3, 4, '线上说明比较完整。', '希望后续增加提醒功能。', 0);

SET FOREIGN_KEY_CHECKS = 1;
