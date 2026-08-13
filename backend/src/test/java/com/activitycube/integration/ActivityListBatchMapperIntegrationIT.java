package com.activitycube.integration;

import com.activitycube.dto.ActivityQueryRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.mapper.UserMapper;
import com.activitycube.service.ActivityService;
import com.activitycube.util.ActivityStatusUtil;
import com.activitycube.util.UserContext;
import com.activitycube.vo.ActivityCountVO;
import com.activitycube.vo.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    private ActivityService activityService;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

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
        feedbackMapper.insert(feedback(activityC.getId(), studentA.getId()));

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

    @Test
    void pagedActivityListFiltersDynamicStatusBeforePaginationAndEnrichesCurrentPage() {
        User organizer = user("organizer", null);
        userMapper.insert(organizer);
        User studentA = user("student", "2026002001");
        userMapper.insert(studentA);
        User studentB = user("student", "2026002002");
        userMapper.insert(studentB);
        LocalDateTime sameCreateTime = LocalDateTime.of(2026, 8, 13, 9, 0);
        String keyword = "分页状态活动-" + UUID.randomUUID();

        Activity registeringA = activityWithTimes(organizer.getId(), sameCreateTime, -2, 2, 3, 5);
        registeringA.setTitle(keyword + "-报名A");
        activityMapper.insert(registeringA);
        Activity registeringB = activityWithTimes(organizer.getId(), sameCreateTime, -3, 3, 4, 6);
        registeringB.setTitle(keyword + "-报名B");
        activityMapper.insert(registeringB);
        Activity registeringC = activityWithTimes(organizer.getId(), sameCreateTime, -4, 4, 5, 7);
        registeringC.setTitle(keyword + "-报名C");
        activityMapper.insert(registeringC);
        Activity waitingStart = activityWithTimes(organizer.getId(), sameCreateTime, -5, -4, 2, 4);
        waitingStart.setTitle(keyword + "-待开始");
        activityMapper.insert(waitingStart);
        Activity ongoing = activityWithTimes(organizer.getId(), sameCreateTime, -5, -4, -1, 2);
        ongoing.setTitle(keyword + "-进行中");
        activityMapper.insert(ongoing);
        Activity ended = activityWithTimes(organizer.getId(), sameCreateTime, -6, -5, -4, -2);
        ended.setTitle(keyword + "-已结束");
        activityMapper.insert(ended);

        Registration registration = registration(registeringC.getId(), studentA, "张三");
        registrationMapper.insert(registration);
        registrationMapper.insert(registration(registeringC.getId(), studentB, "李四"));
        checkinMapper.insert(checkin(registeringC.getId(), studentA.getId(), registration.getId()));
        feedbackMapper.insert(feedback(registeringC.getId(), studentA.getId()));
        UserContext.set(studentA);

        PageResult<Activity> firstPage = activityService.page(query(keyword, ActivityStatusUtil.REGISTERING, 1, 2));
        PageResult<Activity> secondPage = activityService.page(query(keyword, ActivityStatusUtil.REGISTERING, 2, 2));

        assertThat(firstPage.getTotal()).isEqualTo(3);
        assertThat(firstPage.getRecords()).extracting(Activity::getId)
                .containsExactly(registeringC.getId(), registeringB.getId());
        assertThat(secondPage.getTotal()).isEqualTo(3);
        assertThat(secondPage.getRecords()).extracting(Activity::getId)
                .containsExactly(registeringA.getId());
        Activity enriched = firstPage.getRecords().get(0);
        assertThat(enriched.getRegistrationCount()).isEqualTo(2L);
        assertThat(enriched.getCheckinCount()).isEqualTo(1L);
        assertThat(enriched.getRegistered()).isTrue();
        assertThat(enriched.getCheckedIn()).isTrue();
        assertThat(enriched.getFeedbackSubmitted()).isTrue();

        assertThat(activityService.page(query(keyword, ActivityStatusUtil.WAITING_START, 1, 10)).getRecords())
                .extracting(Activity::getId).contains(waitingStart.getId());
        assertThat(activityService.page(query(keyword, ActivityStatusUtil.ONGOING, 1, 10)).getRecords())
                .extracting(Activity::getId).contains(ongoing.getId());
        assertThat(activityService.page(query(keyword, ActivityStatusUtil.ENDED, 1, 10)).getRecords())
                .extracting(Activity::getId).contains(ended.getId());
    }

    @Test
    void pageFiltersByActivityModeBeforePagination() {
        User organizer = user("organizer", null);
        userMapper.insert(organizer);
        User student = user("student", "2026001999");
        userMapper.insert(student);
        String keyword = "mode-page-" + UUID.randomUUID();

        Activity onlineA = activity(organizer.getId());
        onlineA.setTitle(keyword + "-线上A");
        onlineA.setActivityMode("online");
        onlineA.setCampus("线上");
        activityMapper.insert(onlineA);

        Activity onlineB = activity(organizer.getId());
        onlineB.setTitle(keyword + "-线上B");
        onlineB.setActivityMode("online");
        onlineB.setCampus("线上");
        activityMapper.insert(onlineB);

        Activity offline = activity(organizer.getId());
        offline.setTitle(keyword + "-线下");
        offline.setActivityMode("offline");
        offline.setCheckinMode("qr");
        offline.setCampus("龙子湖校区");
        offline.setLocation("龙子湖校区活动室");
        activityMapper.insert(offline);

        UserContext.set(student);
        ActivityQueryRequest request = query(keyword, "ALL", 1, 10);
        request.setActivityMode("online");

        PageResult<Activity> result = activityService.page(request);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getRecords()).extracting(Activity::getId)
                .containsExactly(onlineB.getId(), onlineA.getId());
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

    private Activity activityWithTimes(Long creatorId, LocalDateTime createdAt,
                                       int registerStartOffsetHours, int registerEndOffsetHours,
                                       int startOffsetHours, int endOffsetHours) {
        Activity activity = activity(creatorId);
        LocalDateTime now = LocalDateTime.now();
        activity.setRegisterStartTime(now.plusHours(registerStartOffsetHours));
        activity.setRegisterEndTime(now.plusHours(registerEndOffsetHours));
        activity.setStartTime(now.plusHours(startOffsetHours));
        activity.setEndTime(now.plusHours(endOffsetHours));
        activity.setCheckinStartTime(now.plusHours(startOffsetHours));
        activity.setCheckinEndTime(now.plusHours(endOffsetHours));
        activity.setCreatedAt(createdAt);
        activity.setUpdatedAt(createdAt);
        return activity;
    }

    private ActivityQueryRequest query(String keyword, String status, int pageNum, int pageSize) {
        ActivityQueryRequest request = new ActivityQueryRequest();
        request.setKeyword(keyword);
        request.setStatus(status);
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        return request;
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

    private Feedback feedback(Long activityId, Long userId) {
        Feedback feedback = new Feedback();
        feedback.setActivityId(activityId);
        feedback.setUserId(userId);
        feedback.setFeedbackType("evaluation");
        feedback.setScore(5);
        feedback.setContent("活动体验不错");
        feedback.setSuggestion("继续保持");
        feedback.setHandleStatus("pending");
        feedback.setAnonymous(false);
        feedback.setCreatedAt(LocalDateTime.now());
        return feedback;
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
