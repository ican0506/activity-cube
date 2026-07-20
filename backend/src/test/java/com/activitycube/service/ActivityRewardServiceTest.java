package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.RewardIssueRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.StudentActivityReward;
import com.activitycube.entity.User;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.StudentActivityRewardMapper;
import com.activitycube.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityRewardServiceTest {
    private final StudentActivityRewardMapper rewardMapper = mock(StudentActivityRewardMapper.class);
    private final ActivityService activityService = mock(ActivityService.class);
    private final CheckinMapper checkinMapper = mock(CheckinMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final NoticeService noticeService = mock(NoticeService.class);
    private final ActivityRewardService rewardService = new ActivityRewardService(
            rewardMapper, activityService, checkinMapper, userMapper, operationLogService, noticeService);

    @Test
    void issuesRewardOnlyToCheckedInStudents() {
        Activity activity = endedRewardActivity();
        Checkin checkin = new Checkin();
        checkin.setActivityId(8L);
        checkin.setUserId(3L);
        User student = new User();
        student.setId(3L);
        student.setStudentNo("2321241389");
        student.setRealName("张三");
        when(activityService.requireManageableActivity(8L, admin())).thenReturn(activity);
        when(checkinMapper.selectList(any())).thenReturn(List.of(checkin));
        when(rewardMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectById(3L)).thenReturn(student);

        List<StudentActivityReward> issued = rewardService.issueRewards(8L, new RewardIssueRequest(), admin());

        assertThat(issued).hasSize(1);
        ArgumentCaptor<StudentActivityReward> captor = ArgumentCaptor.forClass(StudentActivityReward.class);
        verify(rewardMapper).insert(captor.capture());
        StudentActivityReward reward = captor.getValue();
        assertThat(reward.getActivityId()).isEqualTo(8L);
        assertThat(reward.getStudentId()).isEqualTo(3L);
        assertThat(reward.getStudentNo()).isEqualTo("2321241389");
        assertThat(reward.getActivityCategory()).isEqualTo("公益活动");
        assertThat(reward.getRewardType()).isEqualTo("课外学时");
        assertThat(reward.getRewardHours()).isEqualByComparingTo("2.0");
        assertThat(reward.getIssuedBy()).isEqualTo(1L);
        assertThat(reward.getStatus()).isEqualTo("issued");
        verify(operationLogService).record(admin(), "issue_activity_reward", "activity", 8L,
                "为活动已签到学生发放活动奖励：校园公益活动；人数：1");
        verify(noticeService).notifyActivityRewardIssued(activity, reward, admin());
    }

    @Test
    void rejectsIssuingRewardBeforeActivityEnded() {
        Activity activity = endedRewardActivity();
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        when(activityService.requireManageableActivity(8L, admin())).thenReturn(activity);

        assertThatThrownBy(() -> rewardService.issueRewards(8L, new RewardIssueRequest(), admin()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动结束后才能发放奖励");
        verify(rewardMapper, never()).insert(any(StudentActivityReward.class));
    }

    @Test
    void rejectsDuplicateRewardIssueWhenAllCheckedStudentsAlreadyHaveReward() {
        Activity activity = endedRewardActivity();
        Checkin checkin = new Checkin();
        checkin.setUserId(3L);
        when(activityService.requireManageableActivity(8L, admin())).thenReturn(activity);
        when(checkinMapper.selectList(any())).thenReturn(List.of(checkin));
        when(rewardMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> rewardService.issueRewards(8L, new RewardIssueRequest(), admin()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该活动奖励已发放，请勿重复发放");
        verify(rewardMapper, never()).insert(any(StudentActivityReward.class));
    }

    @Test
    void rejectsRewardCenterAccessByNonStudentRole() {
        assertThatThrownBy(() -> rewardService.myRewards(admin()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有学生可以查看活动成果");
    }

    private Activity endedRewardActivity() {
        Activity activity = new Activity();
        activity.setId(8L);
        activity.setTitle("校园公益活动");
        activity.setStatus("PUBLISHED");
        activity.setRegisterStartTime(LocalDateTime.now().minusDays(3));
        activity.setRegisterEndTime(LocalDateTime.now().minusDays(2));
        activity.setStartTime(LocalDateTime.now().minusDays(1).minusHours(3));
        activity.setEndTime(LocalDateTime.now().minusDays(1));
        activity.setActivityCategory("公益活动");
        activity.setRewardEnabled(true);
        activity.setRewardType("课外学时");
        activity.setRewardHours(new BigDecimal("2.0"));
        activity.setRewardPoints(0);
        activity.setRewardDescription("完成签到后获得 2 课外学时");
        return activity;
    }

    private User admin() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRealName("管理员");
        user.setRole("admin");
        return user;
    }
}
