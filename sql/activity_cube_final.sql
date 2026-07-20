-- 活动魔方最终完整初始化 SQL
-- 适用：全新 MySQL 数据库初始化。已有旧库请优先执行 docs/sql 下的兼容补丁。

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `activity_cube`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `activity_cube`;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号，学生默认等于学号，负责人默认等于工号',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密后的登录密码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `student_no` VARCHAR(50) DEFAULT NULL COMMENT '学生学号',
  `work_no` VARCHAR(50) DEFAULT NULL COMMENT '负责人工号',
  `grade_year` VARCHAR(20) DEFAULT NULL COMMENT '年级，例如2023级',
  `major_code` VARCHAR(50) DEFAULT NULL COMMENT '专业编码',
  `major_name` VARCHAR(100) DEFAULT NULL COMMENT '专业名称',
  `role` VARCHAR(20) NOT NULL COMMENT '用户角色：student学生/organizer活动负责人/admin管理员',
  `campus` VARCHAR(50) DEFAULT NULL COMMENT '所在校区：龙子湖校区/文化路校区/许昌校区',
  `college` VARCHAR(100) DEFAULT NULL COMMENT '学院或部门',
  `class_name` VARCHAR(100) DEFAULT NULL COMMENT '班级',
  `major_class` VARCHAR(100) DEFAULT NULL COMMENT '兼容旧字段：专业班级',
  `phone` VARCHAR(30) DEFAULT NULL COMMENT '手机号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1启用，0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_student_no` (`student_no`),
  UNIQUE KEY `uk_user_work_no` (`work_no`),
  KEY `idx_user_role` (`role`),
  KEY `idx_user_campus` (`campus`),
  CONSTRAINT `chk_user_role` CHECK (`role` IN ('student', 'organizer', 'admin')),
  CONSTRAINT `chk_user_campus` CHECK (`campus` IS NULL OR `campus` IN ('龙子湖校区', '文化路校区', '许昌校区'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `title` VARCHAR(100) NOT NULL COMMENT '活动名称',
  `description` TEXT NOT NULL COMMENT '活动介绍',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '活动封面图URL',
  `activity_mode` VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '活动形式：online线上活动/offline线下活动',
  `checkin_code` VARCHAR(64) DEFAULT NULL COMMENT '签到码：线下活动现场扫码签到校验使用',
  `campus` VARCHAR(50) NOT NULL COMMENT '活动校区：全校区/龙子湖校区/文化路校区/许昌校区/线上',
  `location` VARCHAR(200) NOT NULL COMMENT '活动地点',
  `start_time` DATETIME NOT NULL COMMENT '活动开始时间',
  `end_time` DATETIME NOT NULL COMMENT '活动结束时间',
  `register_start_time` DATETIME NOT NULL COMMENT '报名开始时间',
  `register_end_time` DATETIME NOT NULL COMMENT '报名结束时间',
  `checkin_start_time` DATETIME NOT NULL COMMENT '签到开始时间',
  `checkin_end_time` DATETIME NOT NULL COMMENT '签到结束时间',
  `max_participants` INT DEFAULT NULL COMMENT '最大报名人数，空表示不限人数',
  `allow_cross_campus` TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许跨校区报名：1允许，0不允许',
  `status` VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '活动工作流状态：DRAFT草稿/PENDING_REVIEW待审核/REJECTED已驳回/PUBLISHED已发布/CANCELLED已取消；展示状态由后端按时间计算',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '活动驳回原因',
  `creator_id` BIGINT NOT NULL COMMENT '创建人用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_creator` (`creator_id`),
  KEY `idx_activity_campus_status` (`campus`, `status`),
  KEY `idx_activity_start_time` (`start_time`),
  KEY `idx_activity_checkin_code` (`checkin_code`),
  CONSTRAINT `fk_activity_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_activity_mode` CHECK (`activity_mode` IN ('online', 'offline')),
  CONSTRAINT `chk_activity_campus` CHECK (`campus` IN ('全校区', '龙子湖校区', '文化路校区', '许昌校区', '线上')),
  CONSTRAINT `chk_activity_workflow_status` CHECK (`status` IN ('DRAFT', 'PENDING_REVIEW', 'REJECTED', 'PUBLISHED', 'CANCELLED')),
  CONSTRAINT `chk_activity_max_participants` CHECK (`max_participants` IS NULL OR `max_participants` > 0),
  CONSTRAINT `chk_activity_time` CHECK (`end_time` > `start_time`),
  CONSTRAINT `chk_activity_register_time` CHECK (`register_end_time` > `register_start_time`),
  CONSTRAINT `chk_activity_checkin_time` CHECK (`checkin_end_time` > `checkin_start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动表';

CREATE TABLE IF NOT EXISTS `registration` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报名记录ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '报名用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '报名姓名',
  `student_no` VARCHAR(50) NOT NULL COMMENT '学号',
  `college` VARCHAR(100) NOT NULL COMMENT '学院',
  `major_class` VARCHAR(100) NOT NULL COMMENT '专业班级',
  `phone` VARCHAR(30) DEFAULT NULL COMMENT '手机号',
  `campus` VARCHAR(50) NOT NULL COMMENT '学生所在校区',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '报名备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_registration_activity_user` (`activity_id`, `user_id`),
  UNIQUE KEY `uk_registration_activity_student_no` (`activity_id`, `student_no`),
  KEY `idx_registration_user` (`user_id`),
  KEY `idx_registration_campus` (`campus`),
  CONSTRAINT `fk_registration_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_registration_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报名表';

CREATE TABLE IF NOT EXISTS `checkin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '签到记录ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '签到用户ID',
  `registration_id` BIGINT NOT NULL COMMENT '报名记录ID',
  `checkin_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  `campus` VARCHAR(50) NOT NULL COMMENT '签到用户所在校区',
  `checkin_type` VARCHAR(16) NOT NULL DEFAULT 'qr' COMMENT '签到方式：qr扫码/online线上/manual人工补签',
  `operator_id` BIGINT DEFAULT NULL COMMENT '人工补签操作人ID',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '人工补签操作人姓名',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '人工补签原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_checkin_activity_user` (`activity_id`, `user_id`),
  KEY `idx_checkin_user` (`user_id`),
  KEY `idx_checkin_registration` (`registration_id`),
  CONSTRAINT `fk_checkin_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_checkin_registration` FOREIGN KEY (`registration_id`) REFERENCES `registration` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_checkin_type` CHECK (`checkin_type` IN ('qr', 'online', 'manual'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到表';

CREATE TABLE IF NOT EXISTS `feedback` (
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
  KEY `idx_feedback_user` (`user_id`),
  CONSTRAINT `fk_feedback_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_feedback_score` CHECK (`score` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈表';

CREATE TABLE IF NOT EXISTS `activity_media` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动媒体ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `media_type` VARCHAR(20) NOT NULL COMMENT '媒体类型：image图片/video视频',
  `usage_type` VARCHAR(20) NOT NULL COMMENT '用途：cover封面/gallery图片墙/video视频',
  `url` VARCHAR(500) NOT NULL COMMENT '文件访问URL',
  `file_name` VARCHAR(255) NOT NULL COMMENT '服务器保存文件名',
  `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  `size` BIGINT DEFAULT NULL COMMENT '文件大小，单位字节',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_media_activity` (`activity_id`),
  KEY `idx_activity_media_usage` (`usage_type`),
  CONSTRAINT `fk_activity_media_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_activity_media_type` CHECK (`media_type` IN ('image', 'video')),
  CONSTRAINT `chk_activity_media_usage` CHECK (`usage_type` IN ('cover', 'gallery', 'video'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动图片视频表';

CREATE TABLE IF NOT EXISTS `feedback_media` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈媒体ID',
  `feedback_id` BIGINT NOT NULL COMMENT '反馈ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `media_type` VARCHAR(20) NOT NULL COMMENT '媒体类型：image图片/video视频',
  `url` VARCHAR(500) NOT NULL COMMENT '文件访问URL',
  `file_name` VARCHAR(255) NOT NULL COMMENT '服务器保存文件名',
  `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  `size` BIGINT DEFAULT NULL COMMENT '文件大小，单位字节',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_media_feedback` (`feedback_id`),
  KEY `idx_feedback_media_activity` (`activity_id`),
  CONSTRAINT `fk_feedback_media_feedback` FOREIGN KEY (`feedback_id`) REFERENCES `feedback` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_feedback_media_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_feedback_media_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_feedback_media_type` CHECK (`media_type` IN ('image', 'video'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈图片视频表';

CREATE TABLE IF NOT EXISTS `lottery_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '抽奖结果ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '中奖用户ID',
  `registration_id` BIGINT NOT NULL COMMENT '报名记录ID',
  `real_name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `student_no` VARCHAR(50) NOT NULL COMMENT '学号',
  `college` VARCHAR(100) DEFAULT NULL COMMENT '学院',
  `class_name` VARCHAR(100) DEFAULT NULL COMMENT '班级',
  `phone` VARCHAR(30) DEFAULT NULL COMMENT '手机号',
  `campus` VARCHAR(50) DEFAULT NULL COMMENT '校区',
  `source` VARCHAR(20) NOT NULL COMMENT '抽奖来源：registration报名名单/checkin已签到名单',
  `round_no` INT NOT NULL COMMENT '抽奖轮次',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_lottery_activity_round` (`activity_id`, `round_no`),
  KEY `idx_lottery_activity_user` (`activity_id`, `user_id`),
  CONSTRAINT `fk_lottery_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_lottery_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_lottery_registration` FOREIGN KEY (`registration_id`) REFERENCES `registration` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_lottery_source` CHECK (`source` IN ('registration', 'checkin')),
  CONSTRAINT `chk_lottery_round_no` CHECK (`round_no` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随机抽奖结果表';

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

CREATE TABLE IF NOT EXISTS `notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `activity_id` BIGINT DEFAULT NULL COMMENT '关联活动ID，系统通知可为空',
  `sender_id` BIGINT DEFAULT NULL COMMENT '发送人ID，系统自动通知可为空',
  `sender_name` VARCHAR(100) DEFAULT NULL COMMENT '发送人姓名',
  `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
  `content` TEXT NOT NULL COMMENT '通知内容',
  `notice_type` VARCHAR(30) NOT NULL COMMENT '通知类型：activity活动通知/checkin_reminder签到提醒/feedback_reminder反馈提醒/system系统通知',
  `target_type` VARCHAR(30) NOT NULL COMMENT '通知对象',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_activity` (`activity_id`),
  KEY `idx_notice_type` (`notice_type`),
  KEY `idx_notice_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

CREATE TABLE IF NOT EXISTS `notice_receiver` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知接收记录ID',
  `notice_id` BIGINT NOT NULL COMMENT '通知ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收人用户ID',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0未读/1已读',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_receiver` (`notice_id`, `receiver_id`),
  KEY `idx_notice_receiver_user` (`receiver_id`, `read_status`, `create_time`),
  CONSTRAINT `fk_notice_receiver_notice` FOREIGN KEY (`notice_id`) REFERENCES `notice` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notice_receiver_user` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知接收表';

INSERT INTO `user`
(`id`, `username`, `password`, `real_name`, `student_no`, `work_no`, `grade_year`, `major_code`, `major_name`, `role`, `campus`, `college`, `class_name`, `major_class`, `phone`, `status`)
VALUES
(1, 'admin', '$2a$10$vHy2YvnwV3xaH2q0VDxeg.kD5L494cwItw3USN38.QADZevM5.Qli', '系统管理员', NULL, NULL, NULL, NULL, NULL, 'admin', '龙子湖校区', '信息化办公室', NULL, '系统管理', '13800000000', 1),
(2, 'T2024001', '$2a$10$vHy2YvnwV3xaH2q0VDxeg.kD5L494cwItw3USN38.QADZevM5.Qli', '李老师', NULL, 'T2024001', NULL, NULL, NULL, 'organizer', '龙子湖校区', '校团委', NULL, '活动负责人', '13800002401', 1),
(3, '2321241389', '$2a$10$vHy2YvnwV3xaH2q0VDxeg.kD5L494cwItw3USN38.QADZevM5.Qli', '张三', '2321241389', NULL, '2023级', '21241', '软件工程', 'student', '龙子湖校区', '信息工程学院', '软件工程2301', '软件工程2301', '13800001389', 1)
ON DUPLICATE KEY UPDATE `password` = VALUES(`password`), `status` = 1, `update_time` = NOW();

INSERT INTO `activity`
(`id`, `title`, `description`, `activity_mode`, `checkin_code`, `campus`, `location`, `start_time`, `end_time`, `register_start_time`, `register_end_time`, `checkin_start_time`, `checkin_end_time`, `max_participants`, `allow_cross_campus`, `status`, `creator_id`)
VALUES
(1, '龙子湖校区校园摄影分享会', '面向龙子湖校区学生的摄影经验分享、作品展示和现场互动活动。', 'offline', REPLACE(UUID(), '-', ''), '龙子湖校区', '龙子湖校区大学生活动中心201', '2026-07-21 14:00:00', '2026-07-21 16:00:00', '2026-07-15 08:00:00', '2026-07-21 12:00:00', '2026-07-21 13:30:00', '2026-07-21 16:30:00', 120, 0, 'PUBLISHED', 2),
(2, '线上AI工具体验课', '介绍常用AI学习与实践工具，活动全程线上进行。', 'online', REPLACE(UUID(), '-', ''), '线上', '腾讯会议', '2026-07-25 19:00:00', '2026-07-25 20:30:00', '2026-07-15 08:00:00', '2026-07-25 18:00:00', '2026-07-25 18:45:00', '2026-07-25 21:00:00', 500, 1, 'PUBLISHED', 2)
ON DUPLICATE KEY UPDATE `update_time` = NOW();

INSERT INTO `registration`
(`id`, `activity_id`, `user_id`, `name`, `student_no`, `college`, `major_class`, `phone`, `campus`, `remark`)
VALUES
(1, 1, 3, '张三', '2321241389', '信息工程学院', '软件工程2301', '13800001389', '龙子湖校区', '喜欢摄影'),
(2, 2, 3, '张三', '2321241389', '信息工程学院', '软件工程2301', '13800001389', '龙子湖校区', NULL)
ON DUPLICATE KEY UPDATE `remark` = VALUES(`remark`);
