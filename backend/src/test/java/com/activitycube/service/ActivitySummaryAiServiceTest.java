package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.ai.ActivitySummaryResult;
import com.activitycube.entity.AiGenerationLog;
import com.activitycube.dto.ai.ActivitySummaryContext;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.User;
import com.activitycube.mapper.AiGenerationLogMapper;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.vo.ActivityStats;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class ActivitySummaryAiServiceTest {
    private final ActivityService activityService = mock(ActivityService.class);
    private final StatService statService = mock(StatService.class);
    private final FeedbackMapper feedbackMapper = mock(FeedbackMapper.class);
    private final AiService aiService = mock(AiService.class);
    private final AiGenerationLogMapper logMapper = mock(AiGenerationLogMapper.class);
    private final ActivitySummaryAiService service = new ActivitySummaryAiService(
            activityService, statService, feedbackMapper, aiService, logMapper, new ObjectMapper());

    @Test
    void buildsSummaryContextFromActivityStatsAndFeedbackWithoutSensitiveStudentData() {
        Activity activity = activity();
        ActivityStats stats = stats();
        Feedback feedback = feedback("组织流程清晰，现场体验很好。联系人手机号 13800000000，学号 2321241389。", 5);
        Feedback longFeedback = feedback("建议下次提前发布物资清单。".repeat(30), null);
        User operator = organizer();
        when(activityService.requireManageableActivity(8L, operator)).thenReturn(activity);
        when(statService.activityStats(8L)).thenReturn(stats);
        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(feedback, longFeedback));

        ActivitySummaryContext context = service.buildSummaryContext(8L, operator);

        assertThat(context.getActivityId()).isEqualTo(8L);
        assertThat(context.getActivityName()).isEqualTo("校园志愿服务活动");
        assertThat(context.getActivityCategory()).isEqualTo("志愿服务");
        assertThat(context.getRegistrationCount()).isEqualTo(20);
        assertThat(context.getCheckinCount()).isEqualTo(16);
        assertThat(context.getCheckinRate()).isEqualTo(80.0);
        assertThat(context.getFeedbackCount()).isEqualTo(2);
        assertThat(context.getAverageScore()).isEqualTo(4.5);
        assertThat(context.getFeedbackTexts()).hasSize(2);
        assertThat(context.getFeedbackTexts().get(0))
                .contains("[手机号已隐藏]")
                .contains("[学号已隐藏]")
                .doesNotContain("13800000000")
                .doesNotContain("2321241389");
        assertThat(context.getFeedbackTexts().get(1).length()).isLessThanOrEqualTo(220);
        verify(statService).activityStats(8L);
    }

    @Test
    void generatesStructuredSummaryAndWritesSuccessLog() {
        User operator = organizer();
        mockContextData(operator);
        when(aiService.completeJson(any())).thenReturn("""
                {
                  "overview": "本次活动围绕校园志愿服务开展，目前可作为阶段性总结。",
                  "dataAnalysis": "活动报名20人，签到16人，签到率80.0%。",
                  "highlights": ["报名参与较积极", "签到转化较稳定", "反馈整体较正向"],
                  "feedbackSummary": "学生反馈集中在组织流程和物资准备。",
                  "problems": ["仍有4人未签到"],
                  "suggestions": ["提前发布物资清单", "加强签到提醒"],
                  "nextAction": "下次活动前完善提醒和现场指引。"
                }
                """);

        ActivitySummaryResult result = service.generateSummary(8L, operator);

        assertThat(result.getOverview()).contains("阶段性总结");
        assertThat(result.getHighlights()).hasSize(3);
        assertThat(result.getProblems()).hasSize(1);
        assertThat(result.getSuggestions()).hasSize(2);
        assertThat(result.getPhaseSummary()).isTrue();
        assertThat(result.getGeneratedAt()).isNotNull();
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).completeJson(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("只能基于真实统计数据")
                .contains("当前为阶段性总结")
                .doesNotContain("13800000000")
                .doesNotContain("2321241389");
        ArgumentCaptor<AiGenerationLog> logCaptor = ArgumentCaptor.forClass(AiGenerationLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getGenerationType()).isEqualTo("ACTIVITY_SUMMARY");
        assertThat(logCaptor.getValue().getActivityId()).isEqualTo(8L);
        assertThat(logCaptor.getValue().getSuccess()).isEqualTo(1);
    }

    @Test
    void malformedSummaryJsonReturnsFriendlyMessageAndWritesFailureLog() {
        User operator = organizer();
        mockContextData(operator);
        when(aiService.completeJson(any())).thenReturn("不是 JSON");

        assertThatThrownBy(() -> service.generateSummary(8L, operator))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 返回格式异常，请稍后重试。");

        ArgumentCaptor<AiGenerationLog> logCaptor = ArgumentCaptor.forClass(AiGenerationLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getGenerationType()).isEqualTo("ACTIVITY_SUMMARY");
        assertThat(logCaptor.getValue().getSuccess()).isZero();
        assertThat(logCaptor.getValue().getErrorMessage()).contains("AI 返回格式异常");
    }

    @Test
    void aiFailureWritesFailureLog() {
        User operator = organizer();
        mockContextData(operator);
        when(aiService.completeJson(any())).thenThrow(new BusinessException("AI 服务暂时不可用，请稍后重试。"));

        assertThatThrownBy(() -> service.generateSummary(8L, operator))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 服务暂时不可用，请稍后重试。");

        ArgumentCaptor<AiGenerationLog> logCaptor = ArgumentCaptor.forClass(AiGenerationLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getSuccess()).isZero();
        assertThat(logCaptor.getValue().getErrorMessage()).contains("AI 服务暂时不可用");
    }

    @Test
    void noDataBuildsZeroValuedContextWithoutCallingModel() {
        User operator = organizer();
        when(activityService.requireManageableActivity(8L, operator)).thenReturn(activity());
        when(statService.activityStats(8L)).thenReturn(new ActivityStats());
        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        ActivitySummaryContext context = service.buildSummaryContext(8L, operator);

        assertThat(context.getRegistrationCount()).isZero();
        assertThat(context.getCheckinCount()).isZero();
        assertThat(context.getCheckinRate()).isZero();
        assertThat(context.getFeedbackCount()).isZero();
        assertThat(context.getAverageScore()).isZero();
        assertThat(context.getFeedbackTexts()).isEmpty();
        verify(aiService, never()).completeJson(any());
    }

    @Test
    void noPermissionStopsBeforeCallingModel() {
        User student = new User();
        student.setId(9L);
        student.setRole("student");
        when(activityService.requireManageableActivity(8L, student))
                .thenThrow(new BusinessException("无活动负责人权限"));

        assertThatThrownBy(() -> service.generateSummary(8L, student))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无活动负责人权限");

        verify(aiService, never()).completeJson(any());
        verify(logMapper, never()).insert(any(AiGenerationLog.class));
    }

    private Activity activity() {
        Activity activity = new Activity();
        activity.setId(8L);
        activity.setTitle("校园志愿服务活动");
        activity.setActivityCategory("志愿服务");
        activity.setCampus("龙子湖校区");
        activity.setStartTime(LocalDateTime.of(2026, 8, 5, 9, 0));
        activity.setEndTime(LocalDateTime.of(2026, 8, 5, 11, 0));
        activity.setStatus("PUBLISHED");
        return activity;
    }

    private ActivityStats stats() {
        ActivityStats stats = new ActivityStats();
        stats.setActivityId(8L);
        stats.setRegistrationCount(20L);
        stats.setCheckinCount(16L);
        stats.setAbsentCount(4L);
        stats.setCheckinRate(80.0);
        stats.setFeedbackCount(2L);
        stats.setAverageScore(4.5);
        return stats;
    }

    private Feedback feedback(String content, Integer score) {
        Feedback feedback = new Feedback();
        feedback.setActivityId(8L);
        feedback.setFeedbackType(score == null ? "suggestion" : "evaluation");
        feedback.setContent(content);
        feedback.setScore(score);
        feedback.setCreatedAt(LocalDateTime.now());
        return feedback;
    }

    private void mockContextData(User operator) {
        when(activityService.requireManageableActivity(8L, operator)).thenReturn(activity());
        when(statService.activityStats(8L)).thenReturn(stats());
        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                feedback("组织流程清晰，现场体验很好。联系人手机号 13800000000，学号 2321241389。", 5),
                feedback("建议下次提前发布物资清单。", null)
        ));
    }

    private User organizer() {
        User user = new User();
        user.setId(2L);
        user.setRole("organizer");
        user.setUsername("T2024001");
        user.setRealName("李老师");
        return user;
    }
}
