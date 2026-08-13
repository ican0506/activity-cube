-- Align feedback schema with current Feedback entity and feedback business types.
-- This migration is intentionally compatible with databases that may have applied
-- docs/sql/feedback_type_patch.sql manually before Flyway V13 existed.

SET @feedback_type_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'feedback'
    AND COLUMN_NAME = 'feedback_type'
);

SET @ddl := IF(
  @feedback_type_exists = 0,
  'ALTER TABLE feedback ADD COLUMN feedback_type VARCHAR(20) NOT NULL DEFAULT ''evaluation'' COMMENT ''反馈类型：suggestion活动建议，issue/problem问题反馈，evaluation活动评价'' AFTER user_id',
  'SELECT ''feedback_type already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @handle_status_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'feedback'
    AND COLUMN_NAME = 'handle_status'
);

SET @ddl := IF(
  @handle_status_exists = 0,
  'ALTER TABLE feedback ADD COLUMN handle_status VARCHAR(20) NOT NULL DEFAULT ''pending'' COMMENT ''处理状态：pending未处理，viewed已查看，resolved已处理'' AFTER suggestion',
  'SELECT ''handle_status already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE feedback
SET feedback_type = 'evaluation'
WHERE feedback_type IS NULL OR feedback_type = '';

UPDATE feedback
SET handle_status = 'pending'
WHERE handle_status IS NULL OR handle_status = '';

UPDATE feedback
SET content = ''
WHERE content IS NULL;

ALTER TABLE feedback MODIFY COLUMN feedback_type VARCHAR(20) NOT NULL DEFAULT 'evaluation' COMMENT '反馈类型：suggestion活动建议，issue/problem问题反馈，evaluation活动评价';
ALTER TABLE feedback MODIFY COLUMN handle_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '处理状态：pending未处理，viewed已查看，resolved已处理';
ALTER TABLE feedback MODIFY COLUMN score INT DEFAULT NULL COMMENT '满意度评分：1-5分，仅活动评价必填';
ALTER TABLE feedback MODIFY COLUMN content VARCHAR(1000) NOT NULL COMMENT '反馈内容';

SET @activity_user_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'feedback'
    AND INDEX_NAME = 'idx_feedback_activity_user'
);

SET @ddl := IF(
  @activity_user_index_exists = 0,
  'ALTER TABLE feedback ADD INDEX idx_feedback_activity_user(activity_id, user_id)',
  'SELECT ''idx_feedback_activity_user already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @old_unique_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'feedback'
    AND INDEX_NAME = 'uk_feedback_activity_user'
);

SET @ddl := IF(
  @old_unique_exists > 0,
  'ALTER TABLE feedback DROP INDEX uk_feedback_activity_user',
  'SELECT ''uk_feedback_activity_user not exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @feedback_type_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'feedback'
    AND INDEX_NAME = 'idx_feedback_type'
);

SET @ddl := IF(
  @feedback_type_index_exists = 0,
  'ALTER TABLE feedback ADD INDEX idx_feedback_type(feedback_type)',
  'SELECT ''idx_feedback_type already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @feedback_user_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'feedback'
    AND INDEX_NAME = 'idx_feedback_user'
);

SET @ddl := IF(
  @feedback_user_index_exists = 0,
  'ALTER TABLE feedback ADD INDEX idx_feedback_user(user_id)',
  'SELECT ''idx_feedback_user already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @chk_feedback_type_exists := (
  SELECT COUNT(*)
  FROM information_schema.CHECK_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND CONSTRAINT_NAME = 'chk_feedback_type'
);

SET @ddl := IF(
  @chk_feedback_type_exists = 0,
  'ALTER TABLE feedback ADD CONSTRAINT chk_feedback_type CHECK (feedback_type IN (''suggestion'', ''issue'', ''problem'', ''evaluation''))',
  'SELECT ''chk_feedback_type already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @chk_feedback_handle_status_exists := (
  SELECT COUNT(*)
  FROM information_schema.CHECK_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND CONSTRAINT_NAME = 'chk_feedback_handle_status'
);

SET @ddl := IF(
  @chk_feedback_handle_status_exists = 0,
  'ALTER TABLE feedback ADD CONSTRAINT chk_feedback_handle_status CHECK (handle_status IN (''pending'', ''viewed'', ''resolved''))',
  'SELECT ''chk_feedback_handle_status already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @chk_feedback_score_exists := (
  SELECT COUNT(*)
  FROM information_schema.CHECK_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND CONSTRAINT_NAME = 'chk_feedback_score'
);

SET @ddl := IF(
  @chk_feedback_score_exists = 0,
  'ALTER TABLE feedback ADD CONSTRAINT chk_feedback_score CHECK (score IS NULL OR score BETWEEN 1 AND 5)',
  'SELECT ''chk_feedback_score already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
