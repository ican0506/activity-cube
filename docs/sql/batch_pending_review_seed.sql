SET NAMES utf8mb4;
USE activity_cube;

-- 批量测试数据：账号 + 待审核活动
-- 说明：
-- 1. 本文件适合在已有 activity_cube 数据库中执行；
-- 2. 不会清空已有数据；
-- 3. 测试账号密码统一为：123456；
-- 4. 活动状态统一为 PENDING_REVIEW，用于测试管理员活动审核流程；
-- 5. 如重复执行，相同 username 的用户会更新基础资料，相同 id 的活动会更新为本文件中的测试数据。

-- BCrypt(123456)
SET @pwd_123456 = '$2a$10$vHy2YvnwV3xaH2q0VDxeg.kD5L494cwItw3USN38.QADZevM5.Qli';

-- 一、活动负责人测试账号
INSERT INTO `user`
(`username`, `password`, `real_name`, `student_no`, `work_no`, `grade_year`, `major_code`, `major_name`, `role`, `campus`, `college`, `class_name`, `major_class`, `phone`, `status`)
VALUES
('T2024101', @pwd_123456, '王老师', NULL, 'T2024101', NULL, NULL, NULL, 'organizer', '龙子湖校区', '信息工程学院', NULL, NULL, '13800001001', 1),
('T2024102', @pwd_123456, '刘老师', NULL, 'T2024102', NULL, NULL, NULL, 'organizer', '文化路校区', '校团委', NULL, NULL, '13800001002', 1),
('T2024103', @pwd_123456, '陈老师', NULL, 'T2024103', NULL, NULL, NULL, 'organizer', '许昌校区', '农学院', NULL, NULL, '13800001003', 1)
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`real_name` = VALUES(`real_name`),
`work_no` = VALUES(`work_no`),
`role` = VALUES(`role`),
`campus` = VALUES(`campus`),
`college` = VALUES(`college`),
`phone` = VALUES(`phone`),
`status` = VALUES(`status`),
`update_time` = CURRENT_TIMESTAMP;

-- 二、学生测试账号
INSERT INTO `user`
(`username`, `password`, `real_name`, `student_no`, `work_no`, `grade_year`, `major_code`, `major_name`, `role`, `campus`, `college`, `class_name`, `major_class`, `phone`, `status`)
VALUES
('2321241001', @pwd_123456, '张小一', '2321241001', NULL, '2023级', '21241', '软件工程', 'student', '龙子湖校区', '信息工程学院', '软件工程2301班', '软件工程2301班', '13900002001', 1),
('2321241002', @pwd_123456, '李小二', '2321241002', NULL, '2023级', '21241', '软件工程', 'student', '龙子湖校区', '信息工程学院', '软件工程2301班', '软件工程2301班', '13900002002', 1),
('2321241003', @pwd_123456, '王小三', '2321241003', NULL, '2023级', '21242', '计算机科学与技术', 'student', '龙子湖校区', '信息工程学院', '计科2301班', '计科2301班', '13900002003', 1),
('2321241004', @pwd_123456, '赵小四', '2321241004', NULL, '2023级', '21242', '计算机科学与技术', 'student', '文化路校区', '信息工程学院', '计科2302班', '计科2302班', '13900002004', 1),
('2321241005', @pwd_123456, '孙小五', '2321241005', NULL, '2023级', '21243', '数据科学与大数据技术', 'student', '文化路校区', '信息工程学院', '数据2301班', '数据2301班', '13900002005', 1),
('2321241006', @pwd_123456, '周小六', '2321241006', NULL, '2023级', '21243', '数据科学与大数据技术', 'student', '文化路校区', '信息工程学院', '数据2301班', '数据2301班', '13900002006', 1),
('2321241007', @pwd_123456, '吴小七', '2321241007', NULL, '2023级', '21241', '软件工程', 'student', '许昌校区', '信息工程学院', '软件工程2302班', '软件工程2302班', '13900002007', 1),
('2321241008', @pwd_123456, '郑小八', '2321241008', NULL, '2023级', '21242', '计算机科学与技术', 'student', '许昌校区', '信息工程学院', '计科2303班', '计科2303班', '13900002008', 1),
('2321241009', @pwd_123456, '冯小九', '2321241009', NULL, '2023级', '21243', '数据科学与大数据技术', 'student', '龙子湖校区', '信息工程学院', '数据2302班', '数据2302班', '13900002009', 1),
('2321241010', @pwd_123456, '陈小十', '2321241010', NULL, '2023级', '21241', '软件工程', 'student', '龙子湖校区', '信息工程学院', '软件工程2303班', '软件工程2303班', '13900002010', 1),
('2321241011', @pwd_123456, '蒋十一', '2321241011', NULL, '2023级', '21242', '计算机科学与技术', 'student', '文化路校区', '信息工程学院', '计科2304班', '计科2304班', '13900002011', 1),
('2321241012', @pwd_123456, '韩十二', '2321241012', NULL, '2023级', '21243', '数据科学与大数据技术', 'student', '许昌校区', '信息工程学院', '数据2303班', '数据2303班', '13900002012', 1)
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`real_name` = VALUES(`real_name`),
`student_no` = VALUES(`student_no`),
`grade_year` = VALUES(`grade_year`),
`major_code` = VALUES(`major_code`),
`major_name` = VALUES(`major_name`),
`role` = VALUES(`role`),
`campus` = VALUES(`campus`),
`college` = VALUES(`college`),
`class_name` = VALUES(`class_name`),
`major_class` = VALUES(`major_class`),
`phone` = VALUES(`phone`),
`status` = VALUES(`status`),
`update_time` = CURRENT_TIMESTAMP;

-- 三、待审核活动测试数据
INSERT INTO `activity`
(`id`, `title`, `description`, `activity_mode`, `checkin_mode`, `activity_category`, `checkin_code`, `campus`, `location`,
 `start_time`, `end_time`, `register_start_time`, `register_end_time`, `checkin_start_time`, `checkin_end_time`,
 `max_participants`, `allow_cross_campus`, `reward_enabled`, `reward_type`, `reward_hours`, `reward_points`, `reward_description`,
 `status`, `reject_reason`, `creator_id`)
VALUES
(1001, '河南农业大学 AI 学习分享会', '面向学生的 AI 工具学习与校园实践分享。', 'online', 'online', '讲座培训', REPLACE(UUID(), '-', ''), '线上', '线上活动',
 '2026-08-01 19:00:00', '2026-08-01 21:00:00', '2026-07-26 08:00:00', '2026-07-31 18:00:00', '2026-08-01 18:30:00', '2026-08-01 21:10:00',
 200, 1, 1, '课外学时', 1.00, 0, '完成签到可获得 1 课外学时。', 'PENDING_REVIEW', NULL, (SELECT id FROM `user` WHERE username = 'T2024101' LIMIT 1)),
(1002, '龙子湖校区志愿服务招募', '组织学生参与校园秩序维护与志愿服务。', 'offline', 'qr', '志愿服务', REPLACE(UUID(), '-', ''), '龙子湖校区', '龙子湖校区图书馆前广场',
 '2026-08-02 09:00:00', '2026-08-02 11:30:00', '2026-07-26 08:00:00', '2026-08-01 18:00:00', '2026-08-02 08:30:00', '2026-08-02 09:30:00',
 80, 1, 1, '课外学时', 2.00, 0, '完成现场签到可获得 2 课外学时。', 'PENDING_REVIEW', NULL, (SELECT id FROM `user` WHERE username = 'T2024101' LIMIT 1)),
(1003, '文化路校区就业经验交流会', '邀请学长学姐分享就业准备和面试经验。', 'offline', 'qr', '讲座培训', REPLACE(UUID(), '-', ''), '文化路校区', '文化路校区报告厅',
 '2026-08-03 15:00:00', '2026-08-03 17:00:00', '2026-07-26 08:00:00', '2026-08-02 18:00:00', '2026-08-03 14:30:00', '2026-08-03 15:30:00',
 120, 1, 1, '积分', 0.00, 10, '完成签到可获得 10 积分。', 'PENDING_REVIEW', NULL, (SELECT id FROM `user` WHERE username = 'T2024102' LIMIT 1)),
(1004, '许昌校区农科实践活动', '围绕农业科技实践开展现场参观与交流。', 'offline', 'qr', '实践活动', REPLACE(UUID(), '-', ''), '许昌校区', '许昌校区实践教学基地',
 '2026-08-04 08:30:00', '2026-08-04 12:00:00', '2026-07-26 08:00:00', '2026-08-03 18:00:00', '2026-08-04 08:00:00', '2026-08-04 09:00:00',
 60, 0, 1, '课外学时', 3.00, 0, '完成签到可获得 3 课外学时。', 'PENDING_REVIEW', NULL, (SELECT id FROM `user` WHERE username = 'T2024103' LIMIT 1)),
(1005, '三校区校园摄影征集说明会', '介绍校园摄影征集活动规则与作品提交方式。', 'hybrid', 'online', '文体活动', REPLACE(UUID(), '-', ''), '全校区', '线上活动',
 '2026-08-05 19:00:00', '2026-08-05 20:30:00', '2026-07-27 08:00:00', '2026-08-04 18:00:00', '2026-08-05 18:30:00', '2026-08-05 20:40:00',
 300, 1, 1, '积分', 0.00, 8, '完成签到可获得 8 积分。', 'PENDING_REVIEW', NULL, (SELECT id FROM `user` WHERE username = 'T2024101' LIMIT 1)),
(1006, '青年志愿者培训', '面向新报名志愿者开展基础服务规范培训。', 'offline', 'qr', '志愿服务', REPLACE(UUID(), '-', ''), '龙子湖校区', '龙子湖校区第一教学楼 210',
 '2026-08-06 14:30:00', '2026-08-06 16:30:00', '2026-07-27 08:00:00', '2026-08-05 18:00:00', '2026-08-06 14:00:00', '2026-08-06 15:00:00',
 100, 1, 1, '课外学时', 2.00, 0, '完成签到可获得 2 课外学时。', 'PENDING_REVIEW', NULL, (SELECT id FROM `user` WHERE username = 'T2024102' LIMIT 1)),
(1007, '学院篮球友谊赛', '组织学院学生开展篮球友谊赛报名。', 'offline', 'qr', '文体活动', REPLACE(UUID(), '-', ''), '文化路校区', '文化路校区篮球场',
 '2026-08-07 17:00:00', '2026-08-07 19:00:00', '2026-07-28 08:00:00', '2026-08-06 18:00:00', '2026-08-07 16:30:00', '2026-08-07 17:30:00',
 40, 0, 0, '无', 0.00, 0, '本活动暂不设置奖励。', 'PENDING_REVIEW', NULL, (SELECT id FROM `user` WHERE username = 'T2024102' LIMIT 1)),
(1008, '创新创业训练营', '围绕项目选题、团队协作、路演表达开展训练。', 'hybrid', 'both', '竞赛活动', REPLACE(UUID(), '-', ''), '全校区', '龙子湖校区创新创业中心',
 '2026-08-08 09:00:00', '2026-08-08 17:00:00', '2026-07-28 08:00:00', '2026-08-07 18:00:00', '2026-08-08 08:30:00', '2026-08-08 09:30:00',
 90, 1, 1, '证书', 0.00, 0, '完成活动并签到可获得电子参与证明。', 'PENDING_REVIEW', NULL, (SELECT id FROM `user` WHERE username = 'T2024101' LIMIT 1)),
(1009, '线上考研经验分享会', '优秀学长学姐分享考研备考规划与复习方法。', 'online', 'online', '讲座培训', REPLACE(UUID(), '-', ''), '线上', '线上活动',
 '2026-08-09 19:30:00', '2026-08-09 21:00:00', '2026-07-29 08:00:00', '2026-08-08 18:00:00', '2026-08-09 19:00:00', '2026-08-09 21:10:00',
 500, 1, 1, '课外学时', 1.00, 0, '完成线上签到可获得 1 课外学时。', 'PENDING_REVIEW', NULL, (SELECT id FROM `user` WHERE username = 'T2024103' LIMIT 1)),
(1010, '校园环保实践', '开展校园垃圾分类宣传和环保实践活动。', 'offline', 'qr', '公益活动', REPLACE(UUID(), '-', ''), '许昌校区', '许昌校区学生服务中心',
 '2026-08-10 09:00:00', '2026-08-10 11:30:00', '2026-07-29 08:00:00', '2026-08-09 18:00:00', '2026-08-10 08:30:00', '2026-08-10 09:30:00',
 70, 1, 1, '课外学时', 2.00, 0, '完成签到可获得 2 课外学时。', 'PENDING_REVIEW', NULL, (SELECT id FROM `user` WHERE username = 'T2024103' LIMIT 1)),
(1011, '混合式社团纳新宣讲', '社团线上线下同步进行纳新说明和答疑。', 'hybrid', 'online', '社团活动', REPLACE(UUID(), '-', ''), '全校区', '线上活动',
 '2026-08-11 18:30:00', '2026-08-11 20:00:00', '2026-07-30 08:00:00', '2026-08-10 18:00:00', '2026-08-11 18:00:00', '2026-08-11 20:10:00',
 260, 1, 0, '无', 0.00, 0, '本活动暂不设置奖励。', 'PENDING_REVIEW', NULL, (SELECT id FROM `user` WHERE username = 'T2024101' LIMIT 1)),
(1012, '数据分析入门讲座', '介绍 Excel、Python 与可视化工具在学习中的应用。', 'online', 'online', '讲座培训', REPLACE(UUID(), '-', ''), '线上', '线上活动',
 '2026-08-12 19:00:00', '2026-08-12 21:00:00', '2026-07-30 08:00:00', '2026-08-11 18:00:00', '2026-08-12 18:30:00', '2026-08-12 21:10:00',
 400, 1, 1, '积分', 0.00, 10, '完成签到可获得 10 积分。', 'PENDING_REVIEW', NULL, (SELECT id FROM `user` WHERE username = 'T2024102' LIMIT 1))
ON DUPLICATE KEY UPDATE
`title` = VALUES(`title`),
`description` = VALUES(`description`),
`activity_mode` = VALUES(`activity_mode`),
`checkin_mode` = VALUES(`checkin_mode`),
`activity_category` = VALUES(`activity_category`),
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
`status` = 'PENDING_REVIEW',
`reject_reason` = NULL,
`creator_id` = VALUES(`creator_id`),
`update_time` = CURRENT_TIMESTAMP;

-- 四、执行后检查
SELECT username, real_name, role, campus, status
FROM `user`
WHERE username IN ('T2024101', 'T2024102', 'T2024103')
   OR student_no BETWEEN '2321241001' AND '2321241012'
ORDER BY role, username;

SELECT id, title, campus, activity_mode, checkin_mode, activity_category, status, creator_id
FROM `activity`
WHERE id BETWEEN 1001 AND 1012
ORDER BY id;
