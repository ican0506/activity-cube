-- Flyway baseline schema for activity_cube.
-- Create the database manually first, then enable FLYWAY_ENABLED=true.

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt密码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `student_no` VARCHAR(50) DEFAULT NULL COMMENT '学号',
  `work_no` VARCHAR(50) DEFAULT NULL COMMENT '工号',
  `grade_year` VARCHAR(20) DEFAULT NULL COMMENT '年级',
  `major_code` VARCHAR(50) DEFAULT NULL COMMENT '专业编码',
  `major_name` VARCHAR(100) DEFAULT NULL COMMENT '专业名称',
  `role` VARCHAR(20) NOT NULL COMMENT '角色',
  `campus` VARCHAR(50) DEFAULT NULL COMMENT '校区',
  `college` VARCHAR(100) DEFAULT NULL COMMENT '学院或部门',
  `class_name` VARCHAR(100) DEFAULT NULL COMMENT '班级',
  `major_class` VARCHAR(100) DEFAULT NULL COMMENT '兼容旧字段：专业班级',
  `phone` VARCHAR(30) DEFAULT NULL COMMENT '手机号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用/0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_student_no` (`student_no`),
  UNIQUE KEY `uk_user_work_no` (`work_no`),
  KEY `idx_user_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `title` VARCHAR(100) NOT NULL COMMENT '活动名称',
  `description` TEXT NOT NULL COMMENT '活动介绍',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `activity_mode` VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '活动形式',
  `checkin_code` VARCHAR(64) DEFAULT NULL COMMENT '签到码',
  `campus` VARCHAR(50) NOT NULL COMMENT '活动校区',
  `location` VARCHAR(200) NOT NULL COMMENT '活动地点',
  `start_time` DATETIME NOT NULL COMMENT '活动开始时间',
  `end_time` DATETIME NOT NULL COMMENT '活动结束时间',
  `register_start_time` DATETIME NOT NULL COMMENT '报名开始时间',
  `register_end_time` DATETIME NOT NULL COMMENT '报名结束时间',
  `checkin_start_time` DATETIME NOT NULL COMMENT '签到开始时间',
  `checkin_end_time` DATETIME NOT NULL COMMENT '签到结束时间',
  `max_participants` INT DEFAULT NULL COMMENT '人数上限',
  `allow_cross_campus` TINYINT NOT NULL DEFAULT 1 COMMENT '允许跨校区',
  `status` VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '工作流状态',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回原因',
  `creator_id` BIGINT NOT NULL COMMENT '创建人ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_creator` (`creator_id`),
  KEY `idx_activity_campus_status` (`campus`, `status`),
  KEY `idx_activity_checkin_code` (`checkin_code`),
  CONSTRAINT `fk_activity_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动表';

CREATE TABLE IF NOT EXISTS `registration` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报名记录ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `student_no` VARCHAR(50) NOT NULL COMMENT '学号',
  `college` VARCHAR(100) NOT NULL COMMENT '学院',
  `major_class` VARCHAR(100) NOT NULL COMMENT '专业班级',
  `phone` VARCHAR(30) DEFAULT NULL COMMENT '手机号',
  `campus` VARCHAR(50) NOT NULL COMMENT '校区',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_registration_activity_user` (`activity_id`, `user_id`),
  UNIQUE KEY `uk_registration_activity_student_no` (`activity_id`, `student_no`),
  KEY `idx_registration_user` (`user_id`),
  CONSTRAINT `fk_registration_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_registration_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报名表';

CREATE TABLE IF NOT EXISTS `checkin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '签到记录ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `registration_id` BIGINT NOT NULL COMMENT '报名记录ID',
  `checkin_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  `campus` VARCHAR(50) NOT NULL COMMENT '校区',
  `checkin_type` VARCHAR(16) NOT NULL DEFAULT 'qr' COMMENT '签到方式',
  `operator_id` BIGINT DEFAULT NULL COMMENT '补签操作人ID',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '补签操作人姓名',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '补签原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_checkin_activity_user` (`activity_id`, `user_id`),
  KEY `idx_checkin_user` (`user_id`),
  KEY `idx_checkin_registration` (`registration_id`),
  CONSTRAINT `fk_checkin_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_checkin_registration` FOREIGN KEY (`registration_id`) REFERENCES `registration` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到表';

CREATE TABLE IF NOT EXISTS `feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `score` INT NOT NULL COMMENT '评分',
  `content` VARCHAR(1000) DEFAULT NULL COMMENT '活动体验',
  `suggestion` VARCHAR(1000) DEFAULT NULL COMMENT '改进建议',
  `anonymous` TINYINT NOT NULL DEFAULT 0 COMMENT '是否匿名',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_feedback_activity_user` (`activity_id`, `user_id`),
  CONSTRAINT `fk_feedback_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈表';

CREATE TABLE IF NOT EXISTS `activity_media` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动媒体ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `media_type` VARCHAR(20) NOT NULL COMMENT '媒体类型',
  `usage_type` VARCHAR(20) NOT NULL COMMENT '用途',
  `url` VARCHAR(500) NOT NULL COMMENT '访问URL',
  `file_name` VARCHAR(255) NOT NULL COMMENT '保存文件名',
  `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原文件名',
  `size` BIGINT DEFAULT NULL COMMENT '大小',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_media_activity` (`activity_id`),
  CONSTRAINT `fk_activity_media_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动媒体表';

CREATE TABLE IF NOT EXISTS `feedback_media` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈媒体ID',
  `feedback_id` BIGINT NOT NULL COMMENT '反馈ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `media_type` VARCHAR(20) NOT NULL COMMENT '媒体类型',
  `url` VARCHAR(500) NOT NULL COMMENT '访问URL',
  `file_name` VARCHAR(255) NOT NULL COMMENT '保存文件名',
  `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原文件名',
  `size` BIGINT DEFAULT NULL COMMENT '大小',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_media_feedback` (`feedback_id`),
  CONSTRAINT `fk_feedback_media_feedback` FOREIGN KEY (`feedback_id`) REFERENCES `feedback` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_feedback_media_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_feedback_media_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈媒体表';

CREATE TABLE IF NOT EXISTS `lottery_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '抽奖结果ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `registration_id` BIGINT NOT NULL COMMENT '报名记录ID',
  `real_name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `student_no` VARCHAR(50) NOT NULL COMMENT '学号',
  `college` VARCHAR(100) DEFAULT NULL COMMENT '学院',
  `class_name` VARCHAR(100) DEFAULT NULL COMMENT '班级',
  `phone` VARCHAR(30) DEFAULT NULL COMMENT '手机号',
  `campus` VARCHAR(50) DEFAULT NULL COMMENT '校区',
  `source` VARCHAR(20) NOT NULL COMMENT '来源',
  `round_no` INT NOT NULL COMMENT '轮次',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_lottery_activity_round` (`activity_id`, `round_no`),
  CONSTRAINT `fk_lottery_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_lottery_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_lottery_registration` FOREIGN KEY (`registration_id`) REFERENCES `registration` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖结果表';

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT NOT NULL COMMENT '操作人ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '账号',
  `role` VARCHAR(20) DEFAULT NULL COMMENT '角色',
  `operation` VARCHAR(64) NOT NULL COMMENT '操作类型',
  `target_type` VARCHAR(32) DEFAULT NULL COMMENT '目标类型',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标ID',
  `detail` VARCHAR(1000) DEFAULT NULL COMMENT '详情',
  `ip` VARCHAR(64) DEFAULT NULL COMMENT 'IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_operation` (`operation`),
  KEY `idx_operation_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

CREATE TABLE IF NOT EXISTS `notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `activity_id` BIGINT DEFAULT NULL COMMENT '活动ID',
  `sender_id` BIGINT DEFAULT NULL COMMENT '发送人ID',
  `sender_name` VARCHAR(100) DEFAULT NULL COMMENT '发送人姓名',
  `title` VARCHAR(100) NOT NULL COMMENT '标题',
  `content` TEXT NOT NULL COMMENT '内容',
  `notice_type` VARCHAR(30) NOT NULL COMMENT '通知类型',
  `target_type` VARCHAR(30) NOT NULL COMMENT '通知对象',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_activity` (`activity_id`),
  KEY `idx_notice_type` (`notice_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

CREATE TABLE IF NOT EXISTS `notice_receiver` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '接收记录ID',
  `notice_id` BIGINT NOT NULL COMMENT '通知ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收人ID',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '已读状态',
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
