package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.ai.ActivityCopywritingRequest;
import com.activitycube.dto.ai.ActivityCopywritingResult;
import com.activitycube.entity.AiGenerationLog;
import com.activitycube.entity.User;
import com.activitycube.mapper.AiGenerationLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityAiServiceTest {
    private final AiService aiService = mock(AiService.class);
    private final AiGenerationLogMapper logMapper = mock(AiGenerationLogMapper.class);
    private final ActivityAiService service = new ActivityAiService(aiService, logMapper, new ObjectMapper());

    @Test
    void parsesStructuredCopywritingJson() {
        when(aiService.completeJson(any())).thenReturn("""
                {
                  "title": "用镜头记录校园之美",
                  "summary": "本次活动面向河南农业大学学生开展，围绕摄影经验、作品展示和现场互动展开。",
                  "highlights": ["摄影经验分享", "优秀作品展示", "现场交流互动"],
                  "socialText": "校园摄影分享会来啦，一起发现农大校园里的光影故事。",
                  "registrationNotice": "请确认活动时间和地点后报名，报名成功后按时参加。"
                }
                """);

        ActivityCopywritingResult result = service.generate(request(), operator());

        assertThat(result.getTitle()).isEqualTo("用镜头记录校园之美");
        assertThat(result.getHighlights()).containsExactly("摄影经验分享", "优秀作品展示", "现场交流互动");
        assertThat(result.getGeneratedAt()).isNotNull();
        ArgumentCaptor<AiGenerationLog> logCaptor = ArgumentCaptor.forClass(AiGenerationLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getSuccess()).isEqualTo(1);
        assertThat(logCaptor.getValue().getInputSummary()).contains("校园摄影分享会").doesNotContain("138");
    }

    @Test
    void rejectsMalformedModelJsonWithFriendlyMessage() {
        when(aiService.completeJson(any())).thenReturn("不是 JSON");

        assertThatThrownBy(() -> service.generate(request(), operator()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 返回格式异常，请稍后重试。");

        ArgumentCaptor<AiGenerationLog> logCaptor = ArgumentCaptor.forClass(AiGenerationLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getSuccess()).isZero();
        assertThat(logCaptor.getValue().getErrorMessage()).contains("AI 返回格式异常");
    }

    @Test
    void requiresActivityName() {
        ActivityCopywritingRequest request = request();
        request.setActivityName(" ");

        assertThatThrownBy(() -> service.generate(request, operator()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先填写活动名称");
    }

    @Test
    void redactsSensitiveValuesFromPromptAndLogSummary() {
        when(aiService.completeJson(any())).thenReturn("""
                {
                  "title": "校园活动标题",
                  "summary": "本次活动面向校园学生开展，内容安排清晰，便于同学们参与。",
                  "highlights": ["活动内容清晰", "组织流程明确", "适合校园参与"],
                  "socialText": "欢迎关注本次校园活动。",
                  "registrationNotice": "请确认活动安排后报名。"
                }
                """);
        ActivityCopywritingRequest request = request();
        request.setHighlights("联系人 13800000000，学号 2321241389 可咨询");
        request.setRegistrationRequirements("手机号 13900000000 不应进入模型");

        service.generate(request, operator());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).completeJson(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .doesNotContain("13800000000")
                .doesNotContain("13900000000")
                .doesNotContain("2321241389");
        ArgumentCaptor<AiGenerationLog> logCaptor = ArgumentCaptor.forClass(AiGenerationLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getInputSummary())
                .doesNotContain("13800000000")
                .doesNotContain("2321241389");
    }

    @Test
    void normalizesHighlightsToThreeItemsForStableFrontendDisplay() {
        when(aiService.completeJson(any())).thenReturn("""
                {
                  "title": "校园活动标题",
                  "summary": "本次活动面向校园学生开展，内容安排清晰，便于同学们参与。",
                  "highlights": ["活动内容清晰"],
                  "socialText": "欢迎关注本次校园活动。",
                  "registrationNotice": "请确认活动安排后报名。"
                }
                """);

        ActivityCopywritingResult result = service.generate(request(), operator());

        assertThat(result.getHighlights()).hasSize(3);
    }

    private ActivityCopywritingRequest request() {
        ActivityCopywritingRequest request = new ActivityCopywritingRequest();
        request.setActivityName("校园摄影分享会");
        request.setActivityType("文体活动");
        request.setCampus("龙子湖校区");
        request.setLocation("大学生活动中心201");
        request.setTargetAudience("在校学生");
        request.setHighlights("摄影经验分享、作品展示、现场互动");
        request.setRegistrationRequirements("对摄影感兴趣即可报名");
        request.setTone("青春");
        return request;
    }

    private User operator() {
        User user = new User();
        user.setId(2L);
        user.setRole("organizer");
        user.setUsername("T2024001");
        user.setRealName("李老师");
        return user;
    }
}
