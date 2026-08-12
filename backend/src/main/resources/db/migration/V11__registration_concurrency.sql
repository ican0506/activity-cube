ALTER TABLE activity
ADD COLUMN registered_count INT NOT NULL DEFAULT 0 COMMENT '当前有效报名人数';

UPDATE activity a
LEFT JOIN (
    SELECT activity_id, COUNT(*) AS cnt
    FROM registration
    GROUP BY activity_id
) r ON r.activity_id = a.id
SET a.registered_count = COALESCE(r.cnt, 0);
