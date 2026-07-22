-- 活动魔方：反馈类型与处理状态字段补丁
-- 适用于已有 activity_cube 数据库升级；不会删除表，不会清空数据。
USE activity_cube;

SET @feedback_type_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'feedback'
    AND COLUMN_NAME = 'feedback_type'
);

SET @ddl := IF(
  @feedback_type_exists = 0,
  'ALTER TABLE feedback ADD COLUMN feedback_type VARCHAR(20) DEFAULT ''evaluation'' COMMENT ''反馈类型：suggestion活动建议，issue问题反馈，evaluation活动评价'' AFTER user_id',
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
  'ALTER TABLE feedback ADD COLUMN handle_status VARCHAR(20) DEFAULT ''pending'' COMMENT ''处理状态：pending未处理，viewed已查看，resolved已处理'' AFTER suggestion',
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
