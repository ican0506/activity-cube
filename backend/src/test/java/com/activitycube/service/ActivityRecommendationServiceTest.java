package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.entity.Activity;
import com.activitycube.entity.ActivityRecommendationLog;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.ActivityRecommendationLogMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.vo.ActivityRecommendationVO;
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
class ActivityRecommendationServiceTest {
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
    private final CheckinMapper checkinMapper = mock(CheckinMapper.class);
    private final ActivityRecommendationLogMapper logMapper = mock(ActivityRecommendationLogMapper.class);
    private final AiService aiService = mock(AiService.class);
    private final ActivityRecommendationService service = new ActivityRecommendationService(
            activityMapper, registrationMapper, checkinMapper, logMapper, aiService, new ObjectMapper());

    @Test
    void recommendsRealPublishedActivitiesAndUsesAiReasonWhenAvailable() {
        User student = student();
        Activity activity = activity(8L, "人工智能前沿讲座", "讲座培训", "龙子湖校区");
        when(activityMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(activity));
        when(registrationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(checkinMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(registrationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(12L, 0L);
        when(aiService.completeJson(any())).thenReturn("""
                {"items":[{"activityId":8,"reason":"与你所在校区和专业方向匹配，活动仍在报名中，适合优先关注。"}]}
                """);
        when(aiService.modelName()).thenReturn("demo-model");

        List<ActivityRecommendationVO> result = service.recommendForStudent(student);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActivityId()).isEqualTo(8L);
        assertThat(result.get(0).getRecommendScore()).isGreaterThan(0);
        assertThat(result.get(0).getAiReason()).contains("适合优先关注");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).completeJson(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("只能解释给定 activities 中真实存在的活动")
                .contains("人工智能前沿讲座")
                .doesNotContain("2321241389")
                .doesNotContain("张三");
        ArgumentCaptor<ActivityRecommendationLog> logCaptor = ArgumentCaptor.forClass(ActivityRecommendationLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActivityId()).isEqualTo(8L);
        assertThat(logCaptor.getValue().getSuccess()).isEqualTo(1);
    }

    @Test
    void aiFailureFallsBackToRuleReasonAndStillReturnsRecommendations() {
        User student = student();
        Activity activity = activity(9L, "校园志愿服务", "志愿服务", "全校区");
        when(activityMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(activity));
        when(registrationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(checkinMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(registrationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L, 0L);
        when(aiService.completeJson(any())).thenThrow(new BusinessException("AI 功能未启用，请配置 AI_ENABLED=true 后再使用。"));

        List<ActivityRecommendationVO> result = service.recommendForStudent(student);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAiReason()).startsWith("推荐该活动");
        ArgumentCaptor<ActivityRecommendationLog> logCaptor = ArgumentCaptor.forClass(ActivityRecommendationLog.class);
        verify(logMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getSuccess()).isZero();
    }

    @Test
    void rejectsNonStudentRoleBeforeQueryingActivities() {
        User organizer = new User();
        organizer.setId(2L);
        organizer.setRole("organizer");

        assertThatThrownBy(() -> service.recommendForStudent(organizer))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前角色不需要学生端活动推荐");

        verify(activityMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    private User student() {
        User user = new User();
        user.setId(1L);
        user.setRole("student");
        user.setUsername("2321241389");
        user.setStudentNo("2321241389");
        user.setRealName("张三");
        user.setCampus("龙子湖校区");
        user.setCollege("信息工程学院");
        user.setMajorName("软件工程");
        return user;
    }

    private Activity activity(Long id, String title, String category, String campus) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setTitle(title);
        activity.setDescription("面向校园学生的主题活动");
        activity.setActivityCategory(category);
        activity.setActivityMode(ActivityService.MODE_OFFLINE);
        activity.setCampus(campus);
        activity.setLocation("第一报告厅");
        activity.setStartTime(LocalDateTime.now().plusDays(3));
        activity.setEndTime(LocalDateTime.now().plusDays(3).plusHours(2));
        activity.setRegisterStartTime(LocalDateTime.now().minusDays(1));
        activity.setRegisterEndTime(LocalDateTime.now().plusDays(1));
        activity.setMaxParticipants(50);
        activity.setAllowCrossCampus(true);
        activity.setStatus("PUBLISHED");
        return activity;
    }
}
