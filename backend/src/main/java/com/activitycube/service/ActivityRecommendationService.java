package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.entity.Activity;
import com.activitycube.entity.ActivityRecommendationLog;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.ActivityRecommendationLogMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.util.ActivityStatusUtil;
import com.activitycube.vo.ActivityRecommendationVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityRecommendationService {
    private static final int MAX_RECOMMENDATIONS = 8;
    private static final int MAX_AI_ITEMS = 6;

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final CheckinMapper checkinMapper;
    private final ActivityRecommendationLogMapper logMapper;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public List<ActivityRecommendationVO> recommendForStudent(User user) {
        requireStudent(user);
        List<Activity> candidates = loadCandidates(user);
        if (candidates.isEmpty()) {
            return List.of();
        }
        UserPreference preference = buildPreference(user);
        List<RecommendationCandidate> scored = candidates.stream()
                .map(activity -> score(activity, user, preference))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingInt(RecommendationCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.activity().getStartTime(), Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_RECOMMENDATIONS)
                .toList();
        if (scored.isEmpty()) {
            return List.of();
        }
        Map<Long, String> aiReasons = generateAiReasons(scored, user, preference);
        List<ActivityRecommendationVO> result = scored.stream()
                .map(candidate -> toVO(candidate, aiReasons.get(candidate.activity().getId())))
                .toList();
        result.forEach(item -> log(user, item, aiReasons.containsKey(item.getActivityId()), null));
        return result;
    }

    private List<Activity> loadCandidates(User user) {
        List<Activity> activities = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, ActivityStatusUtil.PUBLISHED)
                .orderByAsc(Activity::getStartTime));
        return activities.stream()
                .peek(this::applyRuntimeFields)
                .filter(activity -> !hasRegistered(activity.getId(), user.getId()))
                .filter(activity -> !isFull(activity))
                .filter(this::isRecommendableStatus)
                .toList();
    }

    private UserPreference buildPreference(User user) {
        List<Registration> registrations = registrationMapper.selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, user.getId()));
        List<Checkin> checkins = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, user.getId()));
        Set<Long> registrationActivityIds = registrations.stream()
                .map(Registration::getActivityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> checkinActivityIds = checkins.stream()
                .map(Checkin::getActivityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Integer> registrationCategories = categoryWeights(registrationActivityIds, 2);
        Map<String, Integer> checkinCategories = categoryWeights(checkinActivityIds, 3);
        return new UserPreference(registrationCategories, checkinCategories);
    }

    private Map<String, Integer> categoryWeights(Set<Long> activityIds, int weight) {
        if (activityIds.isEmpty()) {
            return Map.of();
        }
        return activityMapper.selectBatchIds(activityIds).stream()
                .map(Activity::getActivityCategory)
                .filter(StringUtils::hasText)
                .collect(Collectors.toMap(category -> category, category -> weight, Integer::sum));
    }

    private RecommendationCandidate score(Activity activity, User user, UserPreference preference) {
        Map<String, Integer> details = new LinkedHashMap<>();
        details.put("campus", campusScore(activity, user));
        details.put("major", majorScore(activity, user));
        details.put("registrationHistory", historyScore(activity.getActivityCategory(), preference.registrationCategories(), 20));
        details.put("checkinHistory", historyScore(activity.getActivityCategory(), preference.checkinCategories(), 15));
        details.put("popularity", popularityScore(activity));
        details.put("time", timeScore(activity));
        int total = details.values().stream().mapToInt(Integer::intValue).sum();
        return new RecommendationCandidate(activity, Math.min(100, total), details);
    }

    private int campusScore(Activity activity, User user) {
        if (ActivityService.MODE_ONLINE.equals(activity.getActivityMode())) {
            return 20;
        }
        if ("全校区".equals(activity.getCampus())) {
            return 22;
        }
        if (StringUtils.hasText(user.getCampus()) && user.getCampus().equals(activity.getCampus())) {
            return 25;
        }
        return Boolean.TRUE.equals(activity.getAllowCrossCampus()) ? 12 : 0;
    }

    private int majorScore(Activity activity, User user) {
        String major = firstText(user.getMajorName(), user.getMajorCode(), user.getCollege());
        if (!StringUtils.hasText(major)) {
            return 3;
        }
        String corpus = (safe(activity.getTitle()) + " " + safe(activity.getDescription()) + " " + safe(activity.getActivityCategory())).toLowerCase();
        if (corpus.contains(major.toLowerCase())) {
            return 15;
        }
        if (isTechnologyMajor(major) && matchesAny(activity.getActivityCategory(), "讲座培训", "竞赛活动", "实践活动")) {
            return 10;
        }
        if (isAgricultureMajor(major) && matchesAny(activity.getActivityCategory(), "实践活动", "志愿服务", "学院活动")) {
            return 10;
        }
        return 4;
    }

    private int historyScore(String category, Map<String, Integer> categoryWeights, int max) {
        if (!StringUtils.hasText(category) || categoryWeights.isEmpty()) {
            return max / 3;
        }
        Integer weight = categoryWeights.get(category);
        if (weight == null) {
            return max / 4;
        }
        return Math.min(max, 6 + weight * 4);
    }

    private int popularityScore(Activity activity) {
        long registrationCount = safeLong(activity.getRegistrationCount());
        Integer maxParticipants = activity.getMaxParticipants();
        if (maxParticipants == null || maxParticipants <= 0) {
            return registrationCount > 0 ? 8 : 5;
        }
        double ratio = registrationCount * 1.0 / maxParticipants;
        if (ratio >= 0.4 && ratio <= 0.8) {
            return 10;
        }
        if (ratio > 0.8) {
            return 6;
        }
        return registrationCount > 0 ? 7 : 5;
    }

    private int timeScore(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (ActivityStatusUtil.REGISTERING.equals(activity.getStatus())) {
            return 10;
        }
        if (activity.getRegisterStartTime() != null && now.isBefore(activity.getRegisterStartTime())) {
            return 6;
        }
        return 4;
    }

    private Map<Long, String> generateAiReasons(List<RecommendationCandidate> candidates, User user, UserPreference preference) {
        List<RecommendationCandidate> aiCandidates = candidates.stream().limit(MAX_AI_ITEMS).toList();
        try {
            String output = aiService.completeJson(buildPrompt(aiCandidates, user, preference));
            return parseReasons(output);
        } catch (BusinessException exception) {
            return Map.of();
        }
    }

    private String buildPrompt(List<RecommendationCandidate> candidates, User user, UserPreference preference) {
        List<Map<String, Object>> activities = candidates.stream()
                .map(candidate -> Map.<String, Object>of(
                        "activityId", candidate.activity().getId(),
                        "title", safe(candidate.activity().getTitle()),
                        "category", safe(candidate.activity().getActivityCategory()),
                        "campus", safe(candidate.activity().getCampus()),
                        "activityMode", safe(candidate.activity().getActivityMode()),
                        "startTime", formatTime(candidate.activity().getStartTime()),
                        "location", displayLocation(candidate.activity()),
                        "recommendScore", candidate.score(),
                        "scoreDetails", candidate.details()
                ))
                .toList();
        Map<String, Object> promptData = new HashMap<>();
        promptData.put("studentProfile", Map.of(
                "campus", safe(user.getCampus()),
                "college", safe(user.getCollege()),
                "majorName", safe(user.getMajorName())
        ));
        promptData.put("historyPreference", Map.of(
                "registrationCategories", preference.registrationCategories(),
                "checkinCategories", preference.checkinCategories()
        ));
        promptData.put("activities", activities);
        String json;
        try {
            json = objectMapper.writeValueAsString(promptData);
        } catch (Exception exception) {
            json = "{}";
        }
        return """
                请为学生端校园活动推荐结果生成推荐理由。

                规则：
                1. 只能解释给定 activities 中真实存在的活动，不得新增、改写或编造活动。
                2. 不得编造时间、地点、奖项、主办单位和学生个人信息。
                3. 推荐理由要简洁，每条 40 到 80 个中文字符。
                4. 输出严格 JSON，不要输出 Markdown。
                5. JSON 格式必须为：
                {
                  "items": [
                    { "activityId": 1, "reason": "推荐理由" }
                  ]
                }

                数据：
                %s
                """.formatted(json);
    }

    private Map<Long, String> parseReasons(String output) {
        try {
            JsonNode root = objectMapper.readTree(output);
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                return Map.of();
            }
            Map<Long, String> reasons = new HashMap<>();
            items.forEach(item -> {
                if (item.path("activityId").canConvertToLong() && item.path("reason").isTextual()) {
                    String reason = item.path("reason").asText().trim();
                    if (StringUtils.hasText(reason)) {
                        reasons.put(item.path("activityId").asLong(), limit(reason, 180));
                    }
                }
            });
            return reasons;
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private ActivityRecommendationVO toVO(RecommendationCandidate candidate, String aiReason) {
        Activity activity = candidate.activity();
        ActivityRecommendationVO vo = new ActivityRecommendationVO();
        vo.setActivityId(activity.getId());
        vo.setTitle(activity.getTitle());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setLocation(displayLocation(activity));
        vo.setCampus(activity.getCampus());
        vo.setActivityMode(activity.getActivityMode());
        vo.setActivityCategory(activity.getActivityCategory());
        vo.setRecommendScore(candidate.score());
        vo.setScoreDetails(candidate.details());
        vo.setRegistrationCount(activity.getRegistrationCount());
        vo.setMaxParticipants(activity.getMaxParticipants());
        vo.setAiReason(StringUtils.hasText(aiReason) ? aiReason : fallbackReason(candidate));
        return vo;
    }

    private String fallbackReason(RecommendationCandidate candidate) {
        Activity activity = candidate.activity();
        List<String> reasons = new ArrayList<>();
        if (candidate.details().getOrDefault("campus", 0) >= 20) {
            reasons.add("校区匹配");
        }
        if (candidate.details().getOrDefault("registrationHistory", 0) >= 10
                || candidate.details().getOrDefault("checkinHistory", 0) >= 10) {
            reasons.add("符合你的历史参与偏好");
        }
        if (candidate.details().getOrDefault("popularity", 0) >= 8) {
            reasons.add("当前报名热度较高");
        }
        if (reasons.isEmpty()) {
            reasons.add("当前仍可报名");
        }
        return "推荐该活动，因为" + String.join("、", reasons) + "。";
    }

    private void log(User user, ActivityRecommendationVO item, boolean aiSuccess, String errorMessage) {
        ActivityRecommendationLog log = new ActivityRecommendationLog();
        log.setUserId(user.getId());
        log.setActivityId(item.getActivityId());
        log.setRecommendScore(item.getRecommendScore());
        try {
            log.setRuleDetail(objectMapper.writeValueAsString(item.getScoreDetails()));
        } catch (Exception exception) {
            log.setRuleDetail("{}");
        }
        log.setAiReason(limit(item.getAiReason(), 1000));
        log.setModelName(aiService.modelName());
        log.setSuccess(aiSuccess ? 1 : 0);
        log.setErrorMessage(limit(errorMessage, 500));
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }

    private void applyRuntimeFields(Activity activity) {
        ActivityStatusUtil.applyCalculatedStatus(activity);
        activity.setRegistrationCount(registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activity.getId())));
    }

    private boolean isRecommendableStatus(Activity activity) {
        return ActivityStatusUtil.REGISTERING.equals(activity.getStatus())
                || ActivityStatusUtil.NOT_STARTED.equals(activity.getStatus())
                || ActivityStatusUtil.WAITING_START.equals(activity.getStatus());
    }

    private boolean isFull(Activity activity) {
        return activity.getMaxParticipants() != null
                && activity.getMaxParticipants() > 0
                && safeLong(activity.getRegistrationCount()) >= activity.getMaxParticipants();
    }

    private boolean hasRegistered(Long activityId, Long userId) {
        return registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getUserId, userId)) > 0;
    }

    private void requireStudent(User user) {
        if (user == null) {
            throw new BusinessException("请先登录");
        }
        if (!"student".equals(user.getRole()) && !"user".equals(user.getRole())) {
            throw new BusinessException("当前角色不需要学生端活动推荐");
        }
    }

    private String displayLocation(Activity activity) {
        if (ActivityService.MODE_ONLINE.equals(activity.getActivityMode()) && !StringUtils.hasText(activity.getLocation())) {
            return "线上活动";
        }
        return StringUtils.hasText(activity.getLocation()) ? activity.getLocation() : "线上活动";
    }

    private boolean matchesAny(String value, String... options) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (String option : options) {
            if (option.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTechnologyMajor(String major) {
        return containsAny(major, "软件", "计算机", "数据", "人工智能", "网络", "信息");
    }

    private boolean isAgricultureMajor(String major) {
        return containsAny(major, "农", "园艺", "植保", "林", "动物", "食品");
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未提供";
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "未提供" : value.toString().replace('T', ' ');
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private record UserPreference(Map<String, Integer> registrationCategories, Map<String, Integer> checkinCategories) {
    }

    private record RecommendationCandidate(Activity activity, int score, Map<String, Integer> details) {
    }
}
