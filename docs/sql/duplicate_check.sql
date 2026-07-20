-- 活动魔方：添加唯一约束前的重复数据检查 SQL
-- 如果任意查询返回数据，请先人工确认保留哪条记录，再删除或合并重复记录。

USE `activity_cube`;

SELECT `username`, COUNT(*) AS duplicate_count
FROM `user`
WHERE `username` IS NOT NULL AND `username` <> ''
GROUP BY `username`
HAVING COUNT(*) > 1;

SELECT `student_no`, COUNT(*) AS duplicate_count
FROM `user`
WHERE `student_no` IS NOT NULL AND `student_no` <> ''
GROUP BY `student_no`
HAVING COUNT(*) > 1;

SELECT `work_no`, COUNT(*) AS duplicate_count
FROM `user`
WHERE `work_no` IS NOT NULL AND `work_no` <> ''
GROUP BY `work_no`
HAVING COUNT(*) > 1;

SELECT `activity_id`, `user_id`, COUNT(*) AS duplicate_count
FROM `registration`
GROUP BY `activity_id`, `user_id`
HAVING COUNT(*) > 1;

SELECT `activity_id`, `student_no`, COUNT(*) AS duplicate_count
FROM `registration`
WHERE `student_no` IS NOT NULL AND `student_no` <> ''
GROUP BY `activity_id`, `student_no`
HAVING COUNT(*) > 1;

SELECT `activity_id`, `user_id`, COUNT(*) AS duplicate_count
FROM `checkin`
GROUP BY `activity_id`, `user_id`
HAVING COUNT(*) > 1;
