package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.LotteryDrawRequest;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.LotteryResult;
import com.activitycube.entity.Registration;
import com.activitycube.mapper.LotteryResultMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LotteryService {
    private static final String SOURCE_REGISTRATION = "registration";
    private static final String SOURCE_CHECKIN = "checkin";

    private final RegistrationService registrationService;
    private final CheckinService checkinService;
    private final LotteryResultMapper lotteryResultMapper;

    public List<LotteryResult> draw(Long activityId, LotteryDrawRequest request) {
        List<LotteryResult> history = history(activityId);
        List<Registration> candidates = candidates(activityId, request.getSource());
        if (!Boolean.TRUE.equals(request.getAllowRepeat())) {
            Set<Long> previousWinnerUserIds = history.stream()
                    .map(LotteryResult::getUserId)
                    .collect(Collectors.toSet());
            candidates = candidates.stream()
                    .filter(registration -> !previousWinnerUserIds.contains(registration.getUserId()))
                    .toList();
        }
        if (candidates.isEmpty()) {
            throw new BusinessException("没有可抽人员");
        }
        if (request.getCount() > candidates.size()) {
            throw new BusinessException("抽取人数不能大于可抽人数");
        }

        List<Registration> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);
        int roundNo = nextRoundNo(history);
        LocalDateTime now = LocalDateTime.now();
        List<LotteryResult> results = shuffled.subList(0, request.getCount()).stream()
                .map(registration -> toResult(activityId, request.getSource(), roundNo, now, registration))
                .toList();
        results.forEach(lotteryResultMapper::insert);
        return results;
    }

    public List<LotteryResult> results(Long activityId) {
        return lotteryResultMapper.selectList(new LambdaQueryWrapper<LotteryResult>()
                .eq(LotteryResult::getActivityId, activityId)
                .orderByDesc(LotteryResult::getRoundNo)
                .orderByDesc(LotteryResult::getCreateTime));
    }

    private List<LotteryResult> history(Long activityId) {
        return lotteryResultMapper.selectList(new LambdaQueryWrapper<LotteryResult>()
                .eq(LotteryResult::getActivityId, activityId));
    }

    private List<Registration> candidates(Long activityId, String source) {
        if (SOURCE_REGISTRATION.equalsIgnoreCase(source)) {
            return registrationService.registrations(activityId);
        }
        if (SOURCE_CHECKIN.equalsIgnoreCase(source)) {
            Set<Long> checkedRegistrationIds = checkinService.checkins(activityId).stream()
                    .map(Checkin::getRegistrationId)
                    .collect(Collectors.toSet());
            return registrationService.registrations(activityId).stream()
                    .filter(registration -> checkedRegistrationIds.contains(registration.getId()))
                    .toList();
        }
        throw new BusinessException("抽奖来源不正确");
    }

    private int nextRoundNo(List<LotteryResult> history) {
        return history.stream()
                .map(LotteryResult::getRoundNo)
                .filter(roundNo -> roundNo != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private LotteryResult toResult(Long activityId, String source, int roundNo, LocalDateTime now, Registration registration) {
        LotteryResult result = new LotteryResult();
        result.setActivityId(activityId);
        result.setUserId(registration.getUserId());
        result.setRegistrationId(registration.getId());
        result.setRealName(registration.getName());
        result.setStudentNo(registration.getStudentNo());
        result.setCollege(registration.getCollege());
        result.setClassName(registration.getMajorClass());
        result.setPhone(registration.getPhone());
        result.setCampus(registration.getCampus());
        result.setSource(source.toLowerCase());
        result.setRoundNo(roundNo);
        result.setCreateTime(now);
        return result;
    }
}
