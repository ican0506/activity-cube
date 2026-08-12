package com.activitycube.integration;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.RegisterRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.mapper.UserMapper;
import com.activitycube.service.RegistrationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RegistrationConcurrencyIntegrationIT {
    private static final String PASSWORD = "$2a$10$vHy2YvnwV3xaH2q0VDxeg.kD5L494cwItw3USN38.QADZevM5.Qli";
    private static final AtomicInteger SEQUENCE = new AtomicInteger(10000);

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
    private RegistrationService registrationService;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private Flyway flyway;
    @Autowired
    private DataSource dataSource;

    @Test
    void flywayMigratesMysqlThroughV11() throws SQLException {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("12");
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).contains("MySQL");
            assertThat(connection.getMetaData().getDatabaseProductVersion()).isNotBlank();
        }
    }

    @Test
    void thirtyDifferentStudentsCompeteForTenSlotsWithoutOverselling() throws Exception {
        Activity activity = createRegisteringActivity(10);
        List<User> users = createStudents(30);
        List<AttemptResult> results = runConcurrently(users.stream()
                .map(user -> (Callable<AttemptResult>) () -> attemptRegister(activity.getId(), user))
                .toList());

        long successCount = results.stream().filter(AttemptResult::successful).count();
        long fullCount = results.stream().filter(result -> "该活动人数已满".equals(result.message())).count();
        List<AttemptResult> unknownFailures = results.stream()
                .filter(result -> !result.successful() && !"该活动人数已满".equals(result.message()))
                .toList();

        assertThat(successCount).isEqualTo(10);
        assertThat(fullCount).isEqualTo(20);
        assertThat(unknownFailures).isEmpty();
        assertCounts(activity.getId(), 10);
    }

    @Test
    void sameStudentConcurrentRegistrationOnlyCreatesOneRegistrationAndOneCounter() throws Exception {
        Activity activity = createRegisteringActivity(100);
        User user = createStudent();
        List<Callable<AttemptResult>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> attemptRegister(activity.getId(), user));
        }

        List<AttemptResult> results = runConcurrently(tasks);

        assertThat(results.stream().filter(AttemptResult::successful).count()).isEqualTo(1);
        assertThat(results.stream()
                .filter(result -> !result.successful())
                .map(AttemptResult::message)
                .allMatch("你已报名，请勿重复提交"::equals)).isTrue();
        assertCounts(activity.getId(), 1);
    }

    @Test
    void fullActivityCanAcceptAnotherStudentAfterSuccessfulCancellation() {
        Activity activity = createRegisteringActivity(1);
        User userA = createStudent();
        User userB = createStudent();

        registrationService.register(activity.getId(), request(userA), userA);
        assertCounts(activity.getId(), 1);

        assertThatThrownBy(() -> registrationService.register(activity.getId(), request(userB), userB))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该活动人数已满");
        assertCounts(activity.getId(), 1);

        registrationService.cancelMyRegistration(activity.getId(), userA);
        assertCounts(activity.getId(), 0);

        registrationService.register(activity.getId(), request(userB), userB);
        assertCounts(activity.getId(), 1);
    }

    @Test
    void registrationInsertFailureRollsBackReservedSlot() {
        Activity activity = createRegisteringActivity(1);
        User user = createStudent();
        RegisterRequest request = request(user);
        request.setName(null);

        assertThatThrownBy(() -> registrationService.register(activity.getId(), request, user))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertCounts(activity.getId(), 0);
    }

    @Test
    void duplicateCancellationDoesNotMakeRegisteredCountNegative() throws Exception {
        Activity activity = createRegisteringActivity(1);
        User user = createStudent();
        registrationService.register(activity.getId(), request(user), user);

        List<Callable<AttemptResult>> tasks = List.of(
                () -> attemptCancel(activity.getId(), user),
                () -> attemptCancel(activity.getId(), user)
        );
        List<AttemptResult> results = runConcurrently(tasks);

        assertThat(results.stream().filter(AttemptResult::successful).count()).isEqualTo(1);
        assertThat(registrationCount(activity.getId())).isZero();
        assertThat(registeredCount(activity.getId())).isZero();
    }

    private AttemptResult attemptRegister(Long activityId, User user) {
        try {
            registrationService.register(activityId, request(user), user);
            return AttemptResult.ok();
        } catch (BusinessException exception) {
            return AttemptResult.fail(exception.getMessage());
        } catch (Exception exception) {
            return AttemptResult.fail(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private AttemptResult attemptCancel(Long activityId, User user) {
        try {
            registrationService.cancelMyRegistration(activityId, user);
            return AttemptResult.ok();
        } catch (BusinessException exception) {
            return AttemptResult.fail(exception.getMessage());
        } catch (Exception exception) {
            return AttemptResult.fail(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private List<AttemptResult> runConcurrently(List<Callable<AttemptResult>> tasks) throws Exception {
        CountDownLatch readyLatch = new CountDownLatch(tasks.size());
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            List<Future<AttemptResult>> futures = tasks.stream()
                    .map(task -> executor.submit(() -> {
                        readyLatch.countDown();
                        if (!startLatch.await(10, TimeUnit.SECONDS)) {
                            return AttemptResult.fail("并发测试启动超时");
                        }
                        return task.call();
                    }))
                    .toList();
            assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();
            List<AttemptResult> results = new ArrayList<>();
            for (Future<AttemptResult> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Activity createRegisteringActivity(Integer maxParticipants) {
        int seq = SEQUENCE.incrementAndGet();
        User organizer = createOrganizer();
        Activity activity = new Activity();
        activity.setTitle("并发报名测试活动-" + seq);
        activity.setDescription("用于真实 MySQL 并发报名集成测试");
        activity.setActivityMode("offline");
        activity.setCheckinMode("qr");
        activity.setActivityCategory("讲座培训");
        activity.setCampus("全校区");
        activity.setLocation("测试教室");
        activity.setStartTime(LocalDateTime.now().plusHours(3));
        activity.setEndTime(LocalDateTime.now().plusHours(5));
        activity.setRegisterStartTime(LocalDateTime.now().minusHours(1));
        activity.setRegisterEndTime(LocalDateTime.now().plusHours(1));
        activity.setCheckinStartTime(LocalDateTime.now().plusHours(2));
        activity.setCheckinEndTime(LocalDateTime.now().plusHours(6));
        activity.setMaxParticipants(maxParticipants);
        activity.setRegisteredCount(0);
        activity.setAllowCrossCampus(true);
        activity.setRewardEnabled(false);
        activity.setRewardType("无");
        activity.setStatus("PUBLISHED");
        activity.setCreatorId(organizer.getId());
        activityMapper.insert(activity);
        return activity;
    }

    private User createOrganizer() {
        int seq = SEQUENCE.incrementAndGet();
        User user = new User();
        user.setUsername("it-organizer-" + seq);
        user.setPassword(PASSWORD);
        user.setRealName("测试负责人" + seq);
        user.setWorkNo("IT-T-" + seq);
        user.setRole("organizer");
        user.setCampus("龙子湖校区");
        user.setCollege("测试学院");
        user.setPhone("139" + seq);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    private List<User> createStudents(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(createStudent());
        }
        return users;
    }

    private User createStudent() {
        int seq = SEQUENCE.incrementAndGet();
        User user = new User();
        user.setUsername("it-student-" + seq);
        user.setPassword(PASSWORD);
        user.setRealName("测试学生" + seq);
        user.setStudentNo("2399" + seq);
        user.setGradeYear("2023级");
        user.setMajorCode("99999");
        user.setMajorName("测试专业");
        user.setRole("student");
        user.setCampus("龙子湖校区");
        user.setCollege("测试学院");
        user.setClassName("测试班");
        user.setMajorClass("测试班");
        user.setPhone("138" + seq);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    private RegisterRequest request(User user) {
        RegisterRequest request = new RegisterRequest();
        request.setName(user.getRealName());
        request.setStudentNo(user.getStudentNo());
        request.setCollege(Objects.requireNonNullElse(user.getCollege(), "测试学院"));
        request.setMajorClass(Objects.requireNonNullElse(user.getMajorClass(), "测试班"));
        request.setPhone(user.getPhone());
        request.setCampus(user.getCampus());
        return request;
    }

    private void assertCounts(Long activityId, long expected) {
        assertThat(registrationCount(activityId)).isEqualTo(expected);
        assertThat(registeredCount(activityId)).isEqualTo(expected);
    }

    private long registrationCount(Long activityId) {
        return registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId));
    }

    private long registeredCount(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        return activity.getRegisteredCount();
    }

    private record AttemptResult(boolean successful, String message) {
        static AttemptResult ok() {
            return new AttemptResult(true, null);
        }

        static AttemptResult fail(String message) {
            return new AttemptResult(false, message);
        }
    }

    private static MysqlSettings mysqlSettings() {
        if ("local".equalsIgnoreCase(setting("it.mysql.mode", "IT_MYSQL_MODE", "testcontainers"))) {
            return localMysqlSettings();
        }
        mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("activity_cube_it")
                .withUsername("activity_cube")
                .withPassword("activity_cube");
        mysql.start();
        return new MysqlSettings(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    }

    private static MysqlSettings localMysqlSettings() {
        String host = setting("it.mysql.host", "IT_MYSQL_HOST", "localhost");
        String port = setting("it.mysql.port", "IT_MYSQL_PORT", "3306");
        String database = setting("it.mysql.database", "IT_MYSQL_DATABASE", "activity_cube_concurrency_it");
        String username = setting("it.mysql.username", "IT_MYSQL_USERNAME", "root");
        String password = setting("it.mysql.password", "IT_MYSQL_PASSWORD", "");
        String rootUrl = "jdbc:mysql://" + host + ":" + port + "/?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection connection = DriverManager.getConnection(rootUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS `" + database + "`");
            statement.executeUpdate("CREATE DATABASE `" + database + "` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci");
        } catch (SQLException exception) {
            throw new IllegalStateException("无法创建本地 MySQL 集成测试库：" + database, exception);
        }
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
        return new MysqlSettings(jdbcUrl, username, password);
    }

    private static String setting(String propertyName, String envName, String defaultValue) {
        String property = System.getProperty(propertyName);
        if (property != null && !property.isBlank()) {
            return property;
        }
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return defaultValue;
    }

    private record MysqlSettings(String jdbcUrl, String username, String password) {
    }
}
