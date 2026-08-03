package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.ai.ActivityCopywritingRequest;
import com.activitycube.dto.ai.ActivityCopywritingResult;
import com.activitycube.entity.AiGenerationLog;
import com.activitycube.entity.User;
import com.activitycube.mapper.AiGenerationLogMapper;
import com.activitycube.util.AuthUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityAiService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AiService aiService;
    private final AiGenerationLogMapper logMapper;
    private final ObjectMapper objectMapper;

    public ActivityCopywritingResult generate(ActivityCopywritingRequest request, User operator) {
        AuthUtil.requireOrganizerOrAdmin(operator);
        if (!StringUtils.hasText(request.getActivityName())) {
            throw new BusinessException("请先填写活动名称");
        }
        String prompt = buildPrompt(request);
        String inputSummary = buildInputSummary(request);
        try {
            String output = aiService.completeJson(prompt);
            ActivityCopywritingResult result = parseResult(output);
            result.setGeneratedAt(LocalDateTime.now());
            log(operator, inputSummary, output, true, null);
            return result;
        } catch (BusinessException exception) {
            log(operator, inputSummary, null, false, exception.getMessage());
            throw exception;
        }
    }

    private ActivityCopywritingResult parseResult(String output) {
        try {
            ActivityCopywritingResult result = objectMapper.readValue(output, ActivityCopywritingResult.class);
            if (!StringUtils.hasText(result.getTitle())
                    || !StringUtils.hasText(result.getSummary())
                    || !StringUtils.hasText(result.getSocialText())
                    || !StringUtils.hasText(result.getRegistrationNotice())) {
                throw new BusinessException("AI 返回格式异常，请稍后重试。");
            }
            if (result.getHighlights() == null || result.getHighlights().isEmpty()) {
                result.setHighlights(List.of("活动内容清晰", "组织流程明确", "适合校园参与"));
            } else {
                result.setHighlights(normalizeHighlights(result.getHighlights()));
            }
            if (result.getSummary().length() > 260) {
                result.setSummary(result.getSummary().substring(0, 260));
            }
            return result;
        } catch (JsonProcessingException exception) {
            throw new BusinessException("AI 返回格式异常，请稍后重试。");
        }
    }

    private String buildPrompt(ActivityCopywritingRequest request) {
        return """
                请为一个高校校园活动生成活动文案。

                规则：
                1. 只基于用户提供的信息生成，不得编造时间、地点、奖项、主办单位。
                2. 信息不足时使用中性表达，不得自行补全事实。
                3. 输出严格 JSON，不要输出 Markdown，不要输出解释文字。
                4. 内容适合高校校园活动场景。
                5. 避免夸大、虚假宣传和敏感内容。
                6. summary 控制在 100 至 200 字左右，不能超过 200 字太多。
                7. highlights 必须返回 3 条活动亮点，每条 8 至 20 个中文字符。
                8. 返回字段必须为 title、summary、highlights、socialText、registrationNotice。
                9. JSON 结构必须严格符合：
                {
                  "title": "宣传标题",
                  "summary": "活动简介",
                  "highlights": ["亮点1", "亮点2", "亮点3"],
                  "socialText": "朋友圈或社群宣传文案",
                  "registrationNotice": "报名须知"
                }

                活动信息：
                活动名称：%s
                活动类型：%s
                校区：%s
                开始时间：%s
                结束时间：%s
                活动地点：%s
                目标人群：%s
                活动亮点：%s
                报名要求：%s
                文案风格：%s
                """.formatted(
                safe(request.getActivityName()),
                safe(request.getActivityType()),
                safe(request.getCampus()),
                formatTime(request.getStartTime()),
                formatTime(request.getEndTime()),
                safe(request.getLocation()),
                safe(request.getTargetAudience()),
                safe(request.getHighlights()),
                safe(request.getRegistrationRequirements()),
                safe(request.getTone())
        );
    }

    private String buildInputSummary(ActivityCopywritingRequest request) {
        return "活动名称=%s；类型=%s；校区=%s；地点=%s；风格=%s".formatted(
                safe(request.getActivityName()),
                safe(request.getActivityType()),
                safe(request.getCampus()),
                safe(request.getLocation()),
                safe(request.getTone())
        );
    }

    private void log(User operator, String inputSummary, String output, boolean success, String errorMessage) {
        AiGenerationLog log = new AiGenerationLog();
        log.setGenerationType("activity_copywriting");
        log.setOperatorId(operator == null ? null : operator.getId());
        log.setModelName(aiService.modelName());
        log.setInputSummary(limit(inputSummary, 1000));
        log.setOutputContent(limit(output, 6000));
        log.setSuccess(success ? 1 : 0);
        log.setErrorMessage(limit(errorMessage, 1000));
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? redactSensitive(value.trim()) : "未提供";
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "未提供" : FORMATTER.format(value);
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private List<String> normalizeHighlights(List<String> highlights) {
        List<String> normalized = new ArrayList<>(highlights.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .limit(3)
                .toList());
        List<String> defaults = List.of("活动内容清晰", "组织流程明确", "适合校园参与");
        for (String item : defaults) {
            if (normalized.size() >= 3) {
                break;
            }
            normalized.add(item);
        }
        return normalized;
    }

    private String redactSensitive(String value) {
        return value
                .replaceAll("1[3-9]\\d{9}", "[手机号已隐藏]")
                .replaceAll("\\b\\d{10}\\b", "[学号已隐藏]");
    }
}
