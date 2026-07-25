SET NAMES utf8mb4;
USE activity_cube;

-- 反馈评分字段兼容补丁
-- 适用场景：
-- 1. 旧数据库中的 feedback.score 仍然是 NOT NULL；
-- 2. 当前产品逻辑已经支持三类反馈：
--    suggestion 活动建议：不需要评分
--    issue 问题反馈：不需要评分
--    evaluation 活动评价：需要 1-5 分评分
--
-- 执行效果：
-- 将 feedback.score 改为允许 NULL，避免提交“活动建议 / 问题反馈”时报：
-- Field 'score' doesn't have a default value
--
-- 安全说明：
-- 1. 不删除 feedback 表；
-- 2. 不清空已有反馈数据；
-- 3. 已有评分数据会保留；
-- 4. 新提交活动评价时，后端仍然会校验评分必须在 1 到 5 之间。

ALTER TABLE `feedback`
  MODIFY COLUMN `score` INT DEFAULT NULL COMMENT '满意度评分：1-5分，仅活动评价必填，活动建议和问题反馈可为空';

-- 执行后检查 score 是否允许为空
SHOW COLUMNS FROM `feedback` LIKE 'score';
