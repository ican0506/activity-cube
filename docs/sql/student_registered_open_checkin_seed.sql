SET NAMES utf8mb4;
USE activity_cube;

-- 测试场景：12 个学生统一报名一个“已发布、可报名、可签到”的活动
-- 说明：
-- 1. 本文件适合在已有 activity_cube 数据库中执行；
-- 2. 不清空业务表，只重置 activity_id = 2001 这个测试活动下 12 个测试学生的签到状态；
-- 3. 执行后，12 个学生都已报名活动 2001，但都未签到，方便测试线上签到、扫码签到、人工补签；
-- 4. 活动 2001 状态为 PUBLISHED，报名时间和签到时间均覆盖当前数据库时间 NOW()；
-- 5. 现场签到码固定为 TESTCHECKIN20260725，方便手动输入测试。

SET @test_activity_id = 2001;
SET @test_checkin_code = 'TESTCHECKIN20260725';

-- 一、确保测试活动存在，并且当前可报名、可签到
INSERT INTO `activity`
(`id`, `title`, `description`, `activity_mode`, `checkin_mode`, `activity_category`, `checkin_code`, `campus`, `location`,
 `start_time`, `end_time`, `register_start_time`, `register_end_time`, `checkin_start_time`, `checkin_end_time`,
 `max_participants`, `allow_cross_campus`, `reward_enabled`, `reward_type`, `reward_hours`, `reward_points`, `reward_description`,
 `status`, `reject_reason`, `creator_id`)
VALUES
(@test_activity_id,
 '签到测试活动：三校区校园服务体验',
 '用于测试学生报名状态、线上签到、扫码签到、人工补签、签到名单和消息通知。',
 'hybrid',
 'both',
 '志愿服务',
 @test_checkin_code,
 '全校区',
 '线上活动',
 DATE_SUB(NOW(), INTERVAL 30 MINUTE),
 DATE_ADD(NOW(), INTERVAL 2 HOUR),
 DATE_SUB(NOW(), INTERVAL 1 DAY),
 DATE_ADD(NOW(), INTERVAL 7 DAY),
 DATE_SUB(NOW(), INTERVAL 15 MINUTE),
 DATE_ADD(NOW(), INTERVAL 2 HOUR),
 200,
 1,
 1,
 '课外学时',
 2.00,
 0,
 '完成签到后可获得 2 课外学时。',
 'PUBLISHED',
 NULL,
 (SELECT id FROM `user` WHERE username = 'T2024101' LIMIT 1))
ON DUPLICATE KEY UPDATE
`title` = VALUES(`title`),
`description` = VALUES(`description`),
`activity_mode` = VALUES(`activity_mode`),
`checkin_mode` = VALUES(`checkin_mode`),
`activity_category` = VALUES(`activity_category`),
`checkin_code` = VALUES(`checkin_code`),
`campus` = VALUES(`campus`),
`location` = VALUES(`location`),
`start_time` = VALUES(`start_time`),
`end_time` = VALUES(`end_time`),
`register_start_time` = VALUES(`register_start_time`),
`register_end_time` = VALUES(`register_end_time`),
`checkin_start_time` = VALUES(`checkin_start_time`),
`checkin_end_time` = VALUES(`checkin_end_time`),
`max_participants` = VALUES(`max_participants`),
`allow_cross_campus` = VALUES(`allow_cross_campus`),
`reward_enabled` = VALUES(`reward_enabled`),
`reward_type` = VALUES(`reward_type`),
`reward_hours` = VALUES(`reward_hours`),
`reward_points` = VALUES(`reward_points`),
`reward_description` = VALUES(`reward_description`),
`status` = 'PUBLISHED',
`reject_reason` = NULL,
`creator_id` = VALUES(`creator_id`),
`update_time` = CURRENT_TIMESTAMP;

-- 二、将 12 个测试学生全部报名到活动 2001
INSERT INTO `registration`
(`activity_id`, `user_id`, `name`, `student_no`, `college`, `major_class`, `phone`, `campus`, `remark`)
SELECT
  @test_activity_id,
  u.id,
  u.real_name,
  u.student_no,
  COALESCE(NULLIF(u.college, ''), '信息工程学院'),
  COALESCE(NULLIF(u.class_name, ''), NULLIF(u.major_class, ''), NULLIF(u.major_name, ''), '测试班级'),
  u.phone,
  COALESCE(NULLIF(u.campus, ''), '龙子湖校区'),
  '批量测试报名'
FROM `user` u
WHERE u.role = 'student'
  AND u.student_no IN (
    '2321241001', '2321241002', '2321241003', '2321241004',
    '2321241005', '2321241006', '2321241007', '2321241008',
    '2321241009', '2321241010', '2321241011', '2321241012'
  )
ON DUPLICATE KEY UPDATE
`name` = VALUES(`name`),
`college` = VALUES(`college`),
`major_class` = VALUES(`major_class`),
`phone` = VALUES(`phone`),
`campus` = VALUES(`campus`),
`remark` = VALUES(`remark`),
`update_time` = CURRENT_TIMESTAMP;

-- 三、重置这 12 个学生在该测试活动下的签到记录，保证可以重新测试签到
DELETE c
FROM `checkin` c
JOIN `user` u ON u.id = c.user_id
WHERE c.activity_id = @test_activity_id
  AND u.student_no IN (
    '2321241001', '2321241002', '2321241003', '2321241004',
    '2321241005', '2321241006', '2321241007', '2321241008',
    '2321241009', '2321241010', '2321241011', '2321241012'
  );

-- 四、执行后检查
SELECT
  id,
  title,
  status,
  activity_mode,
  checkin_mode,
  campus,
  location,
  register_start_time,
  register_end_time,
  checkin_start_time,
  checkin_end_time,
  checkin_code
FROM `activity`
WHERE id = @test_activity_id;

SELECT
  r.activity_id,
  r.name,
  r.student_no,
  r.campus,
  r.create_time AS register_time,
  CASE WHEN c.id IS NULL THEN '未签到' ELSE '已签到' END AS checkin_status
FROM `registration` r
LEFT JOIN `checkin` c ON c.activity_id = r.activity_id AND c.user_id = r.user_id
WHERE r.activity_id = @test_activity_id
ORDER BY r.student_no;
