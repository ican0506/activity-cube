package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.entity.Activity;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.util.ActivityStatusUtil;
import com.activitycube.vo.CheckinCodeResolveResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanService {
    private final ActivityMapper activityMapper;

    public CheckinCodeResolveResult resolveCheckinCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("签到码无效");
        }
        List<Activity> activities = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCheckinCode, code.trim()));
        Activity activity = activities.stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("签到码无效"));

        if (ActivityStatusUtil.CANCELLED.equals(ActivityStatusUtil.calculateStatus(activity))) {
            throw new BusinessException("活动已取消，无法签到");
        }
        LocalDateTime checkinEndTime = activity.getCheckinEndTime() == null
                ? activity.getEndTime()
                : activity.getCheckinEndTime();
        if (checkinEndTime != null && LocalDateTime.now().isAfter(checkinEndTime)) {
            throw new BusinessException("签到码已过期，请重新获取");
        }

        CheckinCodeResolveResult result = new CheckinCodeResolveResult();
        result.setActivityId(activity.getId());
        result.setActivityName(activity.getTitle());
        result.setActivityMode(activity.getActivityMode());
        result.setCheckinCode(activity.getCheckinCode());
        return result;
    }
}
