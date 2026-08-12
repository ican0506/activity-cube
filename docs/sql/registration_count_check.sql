-- 报名人数计数器核对脚本
-- 用途：检查 activity.registered_count 是否与 registration 真实报名明细数量一致。
-- 注意：本脚本默认只查询，不修改数据。

USE `activity_cube`;

SELECT
    a.id,
    a.title,
    a.registered_count,
    COUNT(r.id) AS actual_count
FROM activity a
LEFT JOIN registration r
    ON r.activity_id = a.id
GROUP BY
    a.id,
    a.title,
    a.registered_count
HAVING
    a.registered_count <> COUNT(r.id);

-- 如需修复，请先备份数据库，再手动执行下面 SQL：
--
-- UPDATE activity a
-- LEFT JOIN (
--     SELECT activity_id, COUNT(*) AS cnt
--     FROM registration
--     GROUP BY activity_id
-- ) r ON r.activity_id = a.id
-- SET a.registered_count = COALESCE(r.cnt, 0);
