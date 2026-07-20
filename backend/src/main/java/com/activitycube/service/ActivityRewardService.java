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
import com.activitycube.util.ActivityStatusUtil;
import com.activitycube.vo.StudentRewardView;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityRewardService {
    private final StudentActivityRewardMapper rewardMapper;
    private final ActivityService activityService;
    private final CheckinMapper checkinMapper;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final NoticeService noticeService;

    @Transactional
    public List<StudentActivityReward> issueRewards(Long activityId, RewardIssueRequest request, User operator) {
        Activity activity = activityService.requireManageableActivity(activityId, operator);
        validateIssueActivity(activity);
        List<Checkin> checkins = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activityId));
        if (checkins.isEmpty()) {
            throw new BusinessException("暂无已签到学生可发放奖励");
        }

        List<StudentActivityReward> issued = new ArrayList<>();
        for (Checkin checkin : checkins) {
            if (hasIssued(activityId, checkin.getUserId())) {
                continue;
            }
            User student = userMapper.selectById(checkin.getUserId());
            if (student == null) {
                continue;
            }
            StudentActivityReward reward = buildReward(activity, student, operator, request);
            rewardMapper.insert(reward);
            issued.add(reward);
            noticeService.notifyActivityRewardIssued(activity, reward, operator);
        }
        if (issued.isEmpty()) {
            throw new BusinessException("该活动奖励已发放，请勿重复发放");
        }
        operationLogService.record(operator, "issue_activity_reward", "activity", activityId,
                "为活动已签到学生发放活动奖励：" + activity.getTitle() + "；人数：" + issued.size());
        return issued;
    }

    public List<StudentRewardView> myRewards(User student) {
        if (student == null || (!"student".equals(student.getRole()) && !"user".equals(student.getRole()))) {
            throw new BusinessException("只有学生可以查看活动成果");
        }
        return rewardMapper.selectList(new LambdaQueryWrapper<StudentActivityReward>()
                        .eq(StudentActivityReward::getStudentId, student.getId())
                        .orderByDesc(StudentActivityReward::getIssuedTime))
                .stream()
                .map(this::toView)
                .toList();
    }

    public List<StudentActivityReward> listByActivity(Long activityId, User user) {
        activityService.requireManageableActivity(activityId, user);
        return rewardMapper.selectList(new LambdaQueryWrapper<StudentActivityReward>()
                .eq(StudentActivityReward::getActivityId, activityId)
                .orderByDesc(StudentActivityReward::getIssuedTime));
    }

    private void validateIssueActivity(Activity activity) {
        if (!Boolean.TRUE.equals(activity.getRewardEnabled()) || !StringUtils.hasText(activity.getRewardType())
                || "无".equals(activity.getRewardType())) {
            throw new BusinessException("该活动未设置奖励");
        }
        if (!ActivityStatusUtil.ENDED.equals(ActivityStatusUtil.calculateStatus(activity))) {
            throw new BusinessException("活动结束后才能发放奖励");
        }
    }

    private boolean hasIssued(Long activityId, Long studentId) {
        return rewardMapper.selectCount(new LambdaQueryWrapper<StudentActivityReward>()
                .eq(StudentActivityReward::getActivityId, activityId)
                .eq(StudentActivityReward::getStudentId, studentId)) > 0;
    }

    private StudentActivityReward buildReward(Activity activity, User student, User operator, RewardIssueRequest request) {
        StudentActivityReward reward = new StudentActivityReward();
        reward.setActivityId(activity.getId());
        reward.setStudentId(student.getId());
        reward.setStudentNo(student.getStudentNo());
        reward.setActivityCategory(activity.getActivityCategory());
        reward.setRewardType(activity.getRewardType());
        reward.setRewardHours(activity.getRewardHours());
        reward.setRewardPoints(activity.getRewardPoints());
        reward.setRewardDescription(activity.getRewardDescription());
        reward.setIssuedBy(operator.getId());
        reward.setIssuedByName(StringUtils.hasText(operator.getRealName()) ? operator.getRealName() : operator.getUsername());
        reward.setIssuedTime(LocalDateTime.now());
        reward.setStatus("issued");
        reward.setRemark(request == null ? null : request.getRemark());
        reward.setCreatedAt(LocalDateTime.now());
        return reward;
    }

    private StudentRewardView toView(StudentActivityReward reward) {
        Activity activity = activityService.requireActivity(reward.getActivityId());
        StudentRewardView view = new StudentRewardView();
        view.setId(reward.getId());
        view.setActivityId(reward.getActivityId());
        view.setActivityTitle(activity.getTitle());
        view.setActivityCategory(reward.getActivityCategory());
        view.setStartTime(activity.getStartTime());
        view.setEndTime(activity.getEndTime());
        view.setRewardType(reward.getRewardType());
        view.setRewardHours(reward.getRewardHours());
        view.setRewardPoints(reward.getRewardPoints());
        view.setRewardDescription(reward.getRewardDescription());
        view.setIssuedTime(reward.getIssuedTime());
        view.setStatus(reward.getStatus());
        view.setRemark(reward.getRemark());
        return view;
    }
}
