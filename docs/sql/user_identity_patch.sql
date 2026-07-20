USE `activity_cube`;

SET @schema_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'work_no') = 0,
  'ALTER TABLE `user` ADD COLUMN `work_no` VARCHAR(50) DEFAULT NULL COMMENT ''工号'' AFTER `student_no`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'grade_year') = 0,
  'ALTER TABLE `user` ADD COLUMN `grade_year` VARCHAR(20) DEFAULT NULL COMMENT ''年级，例如2023级'' AFTER `work_no`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'major_code') = 0,
  'ALTER TABLE `user` ADD COLUMN `major_code` VARCHAR(50) DEFAULT NULL COMMENT ''专业编码'' AFTER `grade_year`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'major_name') = 0,
  'ALTER TABLE `user` ADD COLUMN `major_name` VARCHAR(100) DEFAULT NULL COMMENT ''专业名称'' AFTER `major_code`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'class_name') = 0,
  'ALTER TABLE `user` ADD COLUMN `class_name` VARCHAR(100) DEFAULT NULL COMMENT ''班级'' AFTER `college`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'status') = 0,
  'ALTER TABLE `user` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT ''账号状态：1启用/0禁用'' AFTER `phone`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `user`
SET `role` = 'student'
WHERE `role` = 'user';

UPDATE `user`
SET `username` = `student_no`
WHERE `role` = 'student'
  AND `student_no` IS NOT NULL
  AND `student_no` <> ''
  AND (`username` IS NULL OR `username` = '');

UPDATE `user`
SET
  `grade_year` = CONCAT('20', SUBSTRING(`student_no`, 1, 2), '级'),
  `major_code` = SUBSTRING(`student_no`, 3, 5),
  `major_name` = CASE SUBSTRING(`student_no`, 3, 5)
    WHEN '21241' THEN '软件工程'
    WHEN '21242' THEN '计算机科学与技术'
    WHEN '21243' THEN '数据科学与大数据技术'
    ELSE `major_name`
  END,
  `class_name` = COALESCE(`class_name`, `major_class`)
WHERE `role` = 'student'
  AND `student_no` REGEXP '^[0-9]{10}$';

UPDATE `user`
SET
  `work_no` = COALESCE(NULLIF(`work_no`, ''), NULLIF(`student_no`, ''), `username`),
  `username` = COALESCE(NULLIF(`work_no`, ''), NULLIF(`student_no`, ''), `username`),
  `student_no` = NULL
WHERE `role` = 'organizer';

INSERT INTO `user`
(`username`, `password`, `real_name`, `student_no`, `work_no`, `grade_year`, `major_code`, `major_name`, `role`, `campus`, `college`, `class_name`, `major_class`, `phone`, `status`, `create_time`, `update_time`)
SELECT '2321241389', '$2a$10$vHy2YvnwV3xaH2q0VDxeg.kD5L494cwItw3USN38.QADZevM5.Qli', '张三', '2321241389', NULL, '2023级', '21241', '软件工程', 'student', '龙子湖校区', '信息工程学院', '软件工程2301', '软件工程2301', '13800001389', 1, NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `user` WHERE `username` = '2321241389' OR `student_no` = '2321241389'
);

INSERT INTO `user`
(`username`, `password`, `real_name`, `student_no`, `work_no`, `grade_year`, `major_code`, `major_name`, `role`, `campus`, `college`, `class_name`, `major_class`, `phone`, `status`, `create_time`, `update_time`)
SELECT 'T2024001', '$2a$10$vHy2YvnwV3xaH2q0VDxeg.kD5L494cwItw3USN38.QADZevM5.Qli', '李老师', NULL, 'T2024001', NULL, NULL, NULL, 'organizer', '龙子湖校区', '校团委', NULL, '活动负责人', '13800002401', 1, NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `user` WHERE `username` = 'T2024001' OR `work_no` = 'T2024001'
);

UPDATE `user`
SET `username` = 'admin',
    `password` = '$2a$10$vHy2YvnwV3xaH2q0VDxeg.kD5L494cwItw3USN38.QADZevM5.Qli',
    `role` = 'admin',
    `status` = 1,
    `update_time` = NOW()
WHERE `username` = 'admin';

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND INDEX_NAME = 'uk_user_username') = 0,
  'ALTER TABLE `user` ADD UNIQUE KEY `uk_user_username` (`username`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND INDEX_NAME = 'uk_user_student_no') = 0,
  'ALTER TABLE `user` ADD UNIQUE KEY `uk_user_student_no` (`student_no`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND INDEX_NAME = 'uk_user_work_no') = 0,
  'ALTER TABLE `user` ADD UNIQUE KEY `uk_user_work_no` (`work_no`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
