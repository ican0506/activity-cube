-- 活动魔方：随机抽奖结果表
-- 在 Navicat 中选择 activity_cube 数据库后执行，或直接整体执行本文件。

USE `activity_cube`;

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
  KEY `idx_lottery_activity` (`activity_id`),
  KEY `idx_lottery_activity_round` (`activity_id`, `round_no`),
  KEY `idx_lottery_activity_user` (`activity_id`, `user_id`),
  KEY `idx_lottery_registration` (`registration_id`),
  CONSTRAINT `fk_lottery_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_lottery_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_lottery_registration` FOREIGN KEY (`registration_id`) REFERENCES `registration` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_lottery_source` CHECK (`source` IN ('registration', 'checkin')),
  CONSTRAINT `chk_lottery_round_no` CHECK (`round_no` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随机抽奖结果表';
