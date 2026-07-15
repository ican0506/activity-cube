-- 活动魔方：feedback 表兼容补丁
-- 用途：在不删除旧数据的前提下，将基础反馈表补齐为满意度统计需要的结构。
-- 可重复执行：如果字段或约束已存在，会自动跳过。

USE `activity_cube`;

DROP PROCEDURE IF EXISTS `patch_activity_cube_feedback`;

DELIMITER //
CREATE PROCEDURE `patch_activity_cube_feedback`()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.CHECK_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'chk_feedback_rating'
  ) THEN
    ALTER TABLE `feedback` DROP CHECK `chk_feedback_rating`;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feedback'
      AND COLUMN_NAME = 'rating'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feedback'
      AND COLUMN_NAME = 'score'
  ) THEN
    ALTER TABLE `feedback`
      CHANGE COLUMN `rating` `score` INT NOT NULL COMMENT '满意度评分：1-5分';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feedback'
      AND COLUMN_NAME = 'content'
  ) THEN
    ALTER TABLE `feedback`
      MODIFY COLUMN `content` VARCHAR(1000) DEFAULT NULL COMMENT '活动体验';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feedback'
      AND COLUMN_NAME = 'suggestion'
  ) THEN
    ALTER TABLE `feedback`
      ADD COLUMN `suggestion` VARCHAR(1000) DEFAULT NULL COMMENT '改进建议' AFTER `content`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feedback'
      AND COLUMN_NAME = 'anonymous'
  ) THEN
    ALTER TABLE `feedback`
      ADD COLUMN `anonymous` TINYINT NOT NULL DEFAULT 0 COMMENT '是否匿名：1匿名，0实名' AFTER `suggestion`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.CHECK_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'chk_feedback_score'
  ) THEN
    ALTER TABLE `feedback`
      ADD CONSTRAINT `chk_feedback_score` CHECK (`score` BETWEEN 1 AND 5);
  END IF;
END//
DELIMITER ;

CALL `patch_activity_cube_feedback`();
DROP PROCEDURE IF EXISTS `patch_activity_cube_feedback`;
