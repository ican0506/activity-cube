CREATE TABLE IF NOT EXISTS activity_recommendation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '学生用户ID',
    activity_id BIGINT NOT NULL COMMENT '推荐活动ID',
    recommend_score INT NOT NULL COMMENT '推荐分数',
    rule_detail TEXT COMMENT '规则命中详情JSON',
    ai_reason TEXT COMMENT 'AI推荐理由',
    model_name VARCHAR(100) COMMENT '模型名称',
    success TINYINT DEFAULT 1 COMMENT '是否成功：1成功，0失败',
    error_message VARCHAR(500) COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_recommend_user_time (user_id, create_time),
    INDEX idx_recommend_activity (activity_id)
) COMMENT='活动推荐日志表';
