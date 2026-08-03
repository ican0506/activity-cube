CREATE TABLE IF NOT EXISTS `ai_generation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI生成记录ID',
  `generation_type` VARCHAR(50) NOT NULL COMMENT '生成类型',
  `activity_id` BIGINT DEFAULT NULL COMMENT '关联活动ID，可为空',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `model_name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
  `input_summary` VARCHAR(1000) DEFAULT NULL COMMENT '输入摘要，不保存敏感信息',
  `output_content` TEXT COMMENT 'AI输出内容',
  `success` TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功：1成功/0失败',
  `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_generation_type` (`generation_type`),
  KEY `idx_ai_generation_operator` (`operator_id`),
  KEY `idx_ai_generation_activity` (`activity_id`),
  KEY `idx_ai_generation_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成记录表';
