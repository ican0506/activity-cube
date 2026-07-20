-- 活动魔方：测试账号密码 BCrypt 加密补丁
-- 在 Navicat 中连接 activity_cube 数据库后执行。
-- 说明：只更新已知测试账号；其他旧明文账号登录成功后会由后端自动升级为 BCrypt。

USE `activity_cube`;

UPDATE `user`
SET `password` = '$2a$10$vHy2YvnwV3xaH2q0VDxeg.kD5L494cwItw3USN38.QADZevM5.Qli',
    `update_time` = NOW()
WHERE `username` IN ('admin', '2321241389', 'T2024001', 'student001', 'organizer001')
  AND (`password` = '123456' OR `password` NOT LIKE '$2%');
