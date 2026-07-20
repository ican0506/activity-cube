USE `activity_cube`;

CREATE TABLE IF NOT EXISTS `notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `activity_id` BIGINT DEFAULT NULL COMMENT '关联活动ID，系统通知可为空',
  `sender_id` BIGINT DEFAULT NULL COMMENT '发送人ID，系统自动通知可为空',
  `sender_name` VARCHAR(100) DEFAULT NULL COMMENT '发送人姓名',
  `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
  `content` TEXT NOT NULL COMMENT '通知内容',
  `notice_type` VARCHAR(30) NOT NULL COMMENT '通知类型：activity活动通知/checkin_reminder签到提醒/feedback_reminder反馈提醒/system系统通知',
  `target_type` VARCHAR(30) NOT NULL COMMENT '通知对象：all_registered全部报名/checked_in已签到/not_checked_in未签到/all_students全部学生/single_user单个用户',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_activity` (`activity_id`),
  KEY `idx_notice_type` (`notice_type`),
  KEY `idx_notice_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

CREATE TABLE IF NOT EXISTS `notice_receiver` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知接收记录ID',
  `notice_id` BIGINT NOT NULL COMMENT '通知ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收人用户ID',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0未读/1已读',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_receiver_user` (`receiver_id`, `read_status`, `create_time`),
  KEY `idx_notice_receiver_notice` (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知接收表';
