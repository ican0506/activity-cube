-- 活动魔方图片/视频上传功能兼容补丁
-- 执行数据库：activity_cube
-- 说明：只新增字段和表，不删除已有数据。

USE `activity_cube`;

DELIMITER $$

DROP PROCEDURE IF EXISTS `add_activity_cover_url_if_missing`$$
CREATE PROCEDURE `add_activity_cover_url_if_missing`()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'activity'
      AND COLUMN_NAME = 'cover_url'
  ) THEN
    ALTER TABLE `activity`
      ADD COLUMN `cover_url` VARCHAR(500) NULL COMMENT '活动封面图URL' AFTER `description`;
  END IF;
END$$

DELIMITER ;

CALL `add_activity_cover_url_if_missing`();
DROP PROCEDURE IF EXISTS `add_activity_cover_url_if_missing`;

CREATE TABLE IF NOT EXISTS `activity_media` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `media_type` VARCHAR(20) NOT NULL COMMENT '媒体类型：image图片，video视频',
  `usage_type` VARCHAR(20) NOT NULL COMMENT '用途：cover封面，gallery图片墙，video视频',
  `url` VARCHAR(500) NOT NULL COMMENT '文件访问URL',
  `file_name` VARCHAR(255) NOT NULL COMMENT '服务器保存文件名',
  `original_name` VARCHAR(255) NULL COMMENT '原始文件名',
  `size` BIGINT NULL COMMENT '文件大小，单位字节',
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
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `feedback_id` BIGINT NOT NULL COMMENT '反馈ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `media_type` VARCHAR(20) NOT NULL COMMENT '媒体类型：image图片，video视频',
  `url` VARCHAR(500) NOT NULL COMMENT '文件访问URL',
  `file_name` VARCHAR(255) NOT NULL COMMENT '服务器保存文件名',
  `original_name` VARCHAR(255) NULL COMMENT '原始文件名',
  `size` BIGINT NULL COMMENT '文件大小，单位字节',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_media_feedback` (`feedback_id`),
  KEY `idx_feedback_media_activity` (`activity_id`),
  CONSTRAINT `fk_feedback_media_feedback` FOREIGN KEY (`feedback_id`) REFERENCES `feedback` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_feedback_media_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_feedback_media_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_feedback_media_type` CHECK (`media_type` IN ('image', 'video'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈图片视频表';
