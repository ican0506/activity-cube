package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.ai.ActivitySummaryContext;
import com.activitycube.dto.ai.ActivitySummaryResult;
import com.activitycube.entity.Activity;
import com.activitycube.entity.AiGenerationLog;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.User;
import com.activitycube.mapper.AiGenerationLogMapper;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.util.ActivityStatusUtil;
import com.activitycube.vo.ActivityStats;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ActivitySummaryAiService {
    private static final int MAX_FEEDBACK_COUNT = 20;
    private static final int MAX_FEEDBACK_LENGTH = 200;

    private final ActivityService activityService;
    private final StatService statService;
    private final FeedbackMapper feedbackMapper;
    private final AiService aiService;
    private final AiGenerationLogMapper logMapper;
    private final ObjectMapper objectMapper;

    public ActivitySummaryResult generateSummary(Long activityId, User operator) {
        ActivitySummaryContext context = buildSummaryContext(activityId, operator);
        String prompt = buildPrompt(context);
        String inputSummary = buildInputSummary(context);
        try {
            String output = aiService.completeJson(prompt);
            ActivitySummaryResult result = parseResult(output);
            result.setPhaseSummary(context.getPhaseSummary());
            result.setGeneratedAt(java.time.LocalDateTime.now());
            log(activityId, operator, inputSummary, output, true, null);
            return result;
        } catch (BusinessException exception) {
            log(activityId, operator, inputSummary, null, false, exception.getMessage());
            throw exception;
        }
    }

    public ActivitySummaryContext buildSummaryContext(Long activityId, User operator) {
        Activity activity = activityService.requireManageableActivity(activityId, operator);
        ActivityStats stats = statService.activityStats(activityId);
        List<Feedback> feedbacks = safeList(feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getActivityId, activityId)
                .orderByDesc(Feedback::getCreatedAt)));

        ActivitySummaryContext context = new ActivitySummaryContext();
        context.setActivityId(activity.getId());
        context.setActivityName(activity.getTitle());
        context.setActivityCategory(activity.getActivityCategory());
        context.setCampus(activity.getCampus());
        context.setStartTime(activity.getStartTime());
        context.setEndTime(activity.getEndTime());
        context.setStatus(ActivityStatusUtil.calculateStatus(activity));
        context.setPhaseSummary(!ActivityStatusUtil.ENDED.equals(context.getStatus()));
        context.setRegistrationCount(safeLong(stats == null ? null : stats.getRegistrationCount()));
        context.setCheckinCount(safeLong(stats == null ? null : stats.getCheckinCount()));
        context.setAbsentCount(safeLong(stats == null ? null : firstNonNull(stats.getAbsentCount(), stats.getAbsenceCount())));
        context.setCheckinRate(safeDouble(stats == null ? null : stats.getCheckinRate()));
        context.setFeedbackCount(safeLong(stats == null ? null : stats.getFeedbackCount()));
        context.setAverageScore(safeDouble(stats == null ? null : firstNonNull(stats.getAverageScore(), stats.getAverageRating())));
        context.setFeedbackTexts(feedbacks.stream()
                .map(this::toFeedbackSummary)
                .filter(StringUtils::hasText)
                .limit(MAX_FEEDBACK_COUNT)
                .toList());
        return context;
    }

    private String buildPrompt(ActivitySummaryContext context) {
        return """
                请基于以下真实校园活动数据生成活动复盘报告。

                规则：
                1. 只能基于真实统计数据和反馈生成，不得编造报名人数、签到人数、评分、奖项、主办单位或用户意见。
                2. 数据不足时必须明确说明“当前数据不足”，不要自行补充事实。
                3. 输出严格 JSON，不要输出 Markdown，不要输出解释文字。
                4. 语言适合高校活动负责人向老师或管理员汇报，表达客观、克制、可执行。
                5. 活动未结束时，需要在 overview 或 dataAnalysis 中说明“当前为阶段性总结”。
                6. highlights 必须返回 3 条。
                7. problems 最多返回 3 条；suggestions 最多返回 5 条。
                8. JSON 字段必须严格为：
                {
                  "overview": "活动概况",
                  "dataAnalysis": "数据分析",
                  "highlights": ["亮点1", "亮点2", "亮点3"],
                  "feedbackSummary": "用户反馈总结",
                  "problems": ["问题1"],
                  "suggestions": ["建议1"],
                  "nextAction": "下一次活动行动建议"
                }

                活动数据：
                活动ID：%d
                活动名称：%s
                活动类型：%s
                校区：%s
                活动开始时间：%s
                活动结束时间：%s
                当前活动状态：%s
                是否阶段性总结：%s
                报名人数：%d
                签到人数：%d
                未签到人数：%d
                签到率：%.1f%%
                反馈数量：%d
                平均评分：%.1f
                文字反馈摘要：
                %s
                """.formatted(
                context.getActivityId(),
                safe(context.getActivityName()),
                safe(context.getActivityCategory()),
                safe(context.getCampus()),
                formatTime(context.getStartTime()),
                formatTime(context.getEndTime()),
                safe(context.getStatus()),
                Boolean.TRUE.equals(context.getPhaseSummary()) ? "是，当前为阶段性总结" : "否",
                context.getRegistrationCount(),
                context.getCheckinCount(),
                context.getAbsentCount(),
                context.getCheckinRate(),
                context.getFeedbackCount(),
                context.getAverageScore(),
                context.getFeedbackTexts().isEmpty() ? "暂无文字反馈" : String.join("\n", context.getFeedbackTexts())
        );
    }

    private ActivitySummaryResult parseResult(String output) {
        try {
            ActivitySummaryResult result = objectMapper.readValue(output, ActivitySummaryResult.class);
            if (!StringUtils.hasText(result.getOverview())
                    || !StringUtils.hasText(result.getDataAnalysis())
                    || !StringUtils.hasText(result.getFeedbackSummary())
                    || !StringUtils.hasText(result.getNextAction())) {
                throw new BusinessException("AI 返回格式异常，请稍后重试。");
            }
            result.setHighlights(normalizeList(result.getHighlights(), List.of("当前数据不足", "组织流程可复盘", "后续仍可优化"), 3));
            result.setProblems(normalizeList(result.getProblems(), List.of("当前数据不足"), 3));
            result.setSuggestions(normalizeList(result.getSuggestions(), List.of("继续完善活动数据记录"), 5));
            return result;
        } catch (JsonProcessingException exception) {
            throw new BusinessException("AI 返回格式异常，请稍后重试。");
        }
    }

    private List<String> normalizeList(List<String> values, List<String> defaults, int max) {
        List<String> normalized = safeList(values).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .limit(max)
                .toList();
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return defaults.stream().limit(max).toList();
    }

    private String buildInputSummary(ActivitySummaryContext context) {
        return "活动ID=%d；活动名称=%s；报名=%d；签到=%d；反馈=%d；阶段性总结=%s".formatted(
                context.getActivityId(),
                safe(context.getActivityName()),
                context.getRegistrationCount(),
                context.getCheckinCount(),
                context.getFeedbackCount(),
                Boolean.TRUE.equals(context.getPhaseSummary()) ? "是" : "否"
        );
    }

    private void log(Long activityId, User operator, String inputSummary, String output, boolean success, String errorMessage) {
        AiGenerationLog log = new AiGenerationLog();
        log.setGenerationType("ACTIVITY_SUMMARY");
        log.setActivityId(activityId);
        log.setOperatorId(operator == null ? null : operator.getId());
        log.setModelName(aiService.modelName());
        log.setInputSummary(limit(inputSummary, 1000));
        log.setOutputContent(limit(output, 6000));
        log.setSuccess(success ? 1 : 0);
        log.setErrorMessage(limit(errorMessage, 1000));
        log.setCreatedAt(java.time.LocalDateTime.now());
        logMapper.insert(log);
    }

    private String toFeedbackSummary(Feedback feedback) {
        StringBuilder builder = new StringBuilder();
        String content = redactSensitive(firstText(feedback.getContent(), feedback.getSuggestion()));
        if (!StringUtils.hasText(content)) {
            return "";
        }
        builder.append("类型=").append(normalizeFeedbackType(feedback.getFeedbackType()));
        if (feedback.getScore() != null) {
            builder.append("；评分=").append(feedback.getScore());
        }
        builder.append("；内容=").append(limit(content, MAX_FEEDBACK_LENGTH));
        return builder.toString();
    }

    private String normalizeFeedbackType(String feedbackType) {
        if ("suggestion".equals(feedbackType)) {
            return "活动建议";
        }
        if ("problem".equals(feedbackType) || "issue".equals(feedbackType)) {
            return "问题反馈";
        }
        if ("evaluation".equals(feedbackType)) {
            return "活动评价";
        }
        return StringUtils.hasText(feedbackType) ? feedbackType : "未分类反馈";
    }

    private String firstText(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        return StringUtils.hasText(second) ? second.trim() : "";
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? redactSensitive(value.trim()) : "未提供";
    }

    private String formatTime(java.time.LocalDateTime value) {
        return value == null ? "未提供" : value.toString().replace('T', ' ');
    }

    private String redactSensitive(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value
                .replaceAll("1[3-9]\\d{9}", "[手机号已隐藏]")
                .replaceAll("\\b\\d{10}\\b", "[学号已隐藏]");
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private long safeLong(Long value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0 : value;
    }

    private <T> List<T> safeList(List<T> list) {
        return Objects.requireNonNullElse(list, Collections.emptyList());
    }
}
