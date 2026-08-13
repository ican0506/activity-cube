package com.activitycube.integration;

import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.mapper.UserMapper;
import com.activitycube.vo.ActivityCountVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ActivityListBatchMapperIntegrationIT {
    private static final String PASSWORD = "$2a$10$vHy2YvnwV3xaH2q0VDxeg.kD5L494cwItw3USN38.QADZevM5.Qli";
    private static MySQLContainer<?> mysql;

    @DynamicPropertySource
    static void configureMysql(DynamicPropertyRegistry registry) {
        MysqlSettings settings = mysqlSettings();
        registry.add("spring.datasource.url", settings::jdbcUrl);
        registry.add("spring.datasource.username", settings::username);
        registry.add("spring.datasource.password", settings::password);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
        registry.add("security.jwt.secret", () -> "0123456789012345678901234567890123456789012345678901234567890123");
    }

    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private CheckinMapper checkinMapper;
    @Autowired
    private FeedbackMapper feedbackMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void batchCountAndStudentActivityIdQueriesReturnExpectedMappings() {
        User organizer = user("organizer", null);
        userMapper.insert(organizer);
        User studentA = user("student", "2026001001");
        userMapper.insert(studentA);
        User studentB = user("student", "2026001002");
        userMapper.insert(studentB);

        Activity activityA = activity(organizer.getId());
        activityMapper.insert(activityA);
        Activity activityB = activity(organizer.getId());
        activityMapper.insert(activityB);
        Activity activityC = activity(organizer.getId());
        activityMapper.insert(activityC);
        List<Long> activityIds = List.of(activityA.getId(), activityB.getId(), activityC.getId());

        Registration registrationA1 = registration(activityA.getId(), studentA, "张三");
        registrationMapper.insert(registrationA1);
        Registration registrationA2 = registration(activityA.getId(), studentB, "李四");
        registrationMapper.insert(registrationA2);
        Registration registrationC1 = registration(activityC.getId(), studentA, "张三");
        registrationMapper.insert(registrationC1);

        checkinMapper.insert(checkin(activityA.getId(), studentA.getId(), registrationA1.getId()));
        checkinMapper.insert(checkin(activityC.getId(), studentA.getId(), registrationC1.getId()));
        insertFeedback(activityC.getId(), studentA.getId());

        Map<Long, Long> registrationCounts = countMap(registrationMapper.countByActivityIds(activityIds));
        Map<Long, Long> checkinCounts = countMap(checkinMapper.countByActivityIds(activityIds));

        assertThat(registrationCounts).containsEntry(activityA.getId(), 2L)
                .containsEntry(activityC.getId(), 1L)
                .doesNotContainKey(activityB.getId());
        assertThat(checkinCounts).containsEntry(activityA.getId(), 1L)
                .containsEntry(activityC.getId(), 1L)
                .doesNotContainKey(activityB.getId());
        assertThat(registrationMapper.findActivityIdsByUserAndActivityIds(studentA.getId(), activityIds))
                .containsExactlyInAnyOrder(activityA.getId(), activityC.getId());
        assertThat(checkinMapper.findActivityIdsByUserAndActivityIds(studentA.getId(), activityIds))
                .containsExactlyInAnyOrder(activityA.getId(), activityC.getId());
        assertThat(feedbackMapper.findActivityIdsByUserAndActivityIds(studentA.getId(), activityIds))
                .containsExactly(activityC.getId());
    }

    private Map<Long, Long> countMap(List<ActivityCountVO> rows) {
        return rows.stream().collect(Collectors.toMap(ActivityCountVO::getActivityId, ActivityCountVO::getCount));
    }

    private Activity activity(Long creatorId) {
        LocalDateTime now = LocalDateTime.now();
        Activity activity = new Activity();
        activity.setTitle("批量查询活动" + UUID.randomUUID());
        activity.setDescription("批量查询测试");
        activity.setActivityMode("online");
        activity.setCheckinMode("online");
        activity.setActivityCategory("讲座培训");
        activity.setCampus("线上");
        activity.setLocation("线上活动");
        activity.setStartTime(now.plusHours(2));
        activity.setEndTime(now.plusHours(4));
        activity.setRegisterStartTime(now.minusHours(1));
        activity.setRegisterEndTime(now.plusHours(1));
        activity.setCheckinStartTime(now.plusHours(2));
        activity.setCheckinEndTime(now.plusHours(4));
        activity.setMaxParticipants(100);
        activity.setRegisteredCount(0);
        activity.setAllowCrossCampus(true);
        activity.setRewardEnabled(false);
        activity.setRewardType("无");
        activity.setStatus("PUBLISHED");
        activity.setCreatorId(creatorId);
        return activity;
    }

    private User user(String role, String studentNo) {
        User user = new User();
        user.setUsername(role + "-" + UUID.randomUUID());
        user.setPassword(PASSWORD);
        user.setRealName("测试用户");
        user.setStudentNo(studentNo);
        user.setRole(role);
        user.setCampus("龙子湖校区");
        user.setCollege("信息工程学院");
        user.setStatus(1);
        return user;
    }

    private Registration registration(Long activityId, User user, String name) {
        Registration registration = new Registration();
        registration.setActivityId(activityId);
        registration.setUserId(user.getId());
        registration.setName(name);
        registration.setStudentNo(user.getStudentNo());
        registration.setCollege("信息工程学院");
        registration.setMajorClass("软件工程");
        registration.setPhone("13800000000");
        registration.setCampus(user.getCampus());
        return registration;
    }

    private Checkin checkin(Long activityId, Long userId, Long registrationId) {
        Checkin checkin = new Checkin();
        checkin.setActivityId(activityId);
        checkin.setUserId(userId);
        checkin.setRegistrationId(registrationId);
        checkin.setCheckinTime(LocalDateTime.now());
        checkin.setCampus("龙子湖校区");
        checkin.setCheckinType("online");
        return checkin;
    }

    private void insertFeedback(Long activityId, Long userId) {
        jdbcTemplate.update("""
                INSERT INTO feedback (activity_id, user_id, score, content, anonymous)
                VALUES (?, ?, ?, ?, ?)
                """, activityId, userId, 5, "活动体验不错", 0);
    }

    private static MysqlSettings mysqlSettings() {
        String mode = setting("it.mysql.mode", "IT_MYSQL_MODE", "testcontainers");
        if ("local".equalsIgnoreCase(mode)) {
            return localMysqlSettings();
        }
        mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("activity_cube_batch_mapper_it")
                .withUsername("activity_cube")
                .withPassword("activity_cube");
        mysql.start();
        return new MysqlSettings(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    }

    private static MysqlSettings localMysqlSettings() {
        String host = setting("it.mysql.host", "IT_MYSQL_HOST", "localhost");
        String port = setting("it.mysql.port", "IT_MYSQL_PORT", "3306");
        String database = setting("it.mysql.database", "IT_MYSQL_DATABASE", "activity_cube_batch_mapper_it");
        String username = setting("it.mysql.username", "IT_MYSQL_USERNAME", "root");
        String password = setting("it.mysql.password", "IT_MYSQL_PASSWORD", "");
        String rootUrl = "jdbc:mysql://" + host + ":" + port + "/?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
        try (Connection connection = DriverManager.getConnection(rootUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS `" + database + "`");
            statement.executeUpdate("CREATE DATABASE `" + database + "` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci");
        } catch (Exception e) {
            throw new IllegalStateException("无法创建本地 MySQL 集成测试库：" + database, e);
        }
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
        return new MysqlSettings(jdbcUrl, username, password);
    }

    private static String setting(String property, String env, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(env);
        }
        return value == null || value.isBlank() ? fallback : value;
    }

    private record MysqlSettings(String jdbcUrl, String username, String password) {
    }
}
