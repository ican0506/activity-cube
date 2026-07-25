package com.activitycube.service;

import com.activitycube.entity.Checkin;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.Registration;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.vo.ActivityStats;
import com.activitycube.vo.CampusStat;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatService {
    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final CheckinService checkinService;
    private final FeedbackMapper feedbackMapper;

    public ActivityStats activityStats(Long activityId) {
        activityService.requireActivity(activityId);
        List<Registration> registrations = safeList(registrationService.registrations(activityId));
        List<Checkin> checkins = safeList(checkinService.checkins(activityId));
        List<Feedback> feedbacks = safeList(feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getActivityId, activityId)));

        long registrationCount = registrations.size();
        long checkinCount = checkins.size();

        Map<String, Long> registrationByCampus = registrations.stream()
                .collect(Collectors.groupingBy(registration -> campusOrDefault(registration.getCampus()), Collectors.counting()));
        Map<String, Long> checkinByCampus = checkins.stream()
                .collect(Collectors.groupingBy(checkin -> campusOrDefault(checkin.getCampus()), Collectors.counting()));

        List<CampusStat> campusStats = registrationByCampus.keySet().stream()
                .map(campus -> new CampusStat(campus, registrationByCampus.getOrDefault(campus, 0L), checkinByCampus.getOrDefault(campus, 0L)))
                .toList();
        double averageScore = feedbacks.stream()
                .map(Feedback::getScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
        long absentCount = Math.max(0, registrationCount - checkinCount);

        ActivityStats stats = new ActivityStats();
        stats.setActivityId(activityId);
        stats.setRegistrationCount(registrationCount);
        stats.setCheckinCount(checkinCount);
        stats.setAbsentCount(absentCount);
        stats.setAbsenceCount(absentCount);
        stats.setCheckinRate(rate(checkinCount, registrationCount));
        stats.setRegistrationToCheckinRate(rate(checkinCount, registrationCount));
        stats.setCheckinToFeedbackRate(rate(feedbacks.size(), checkinCount));
        stats.setRegistrationToFeedbackRate(rate(feedbacks.size(), registrationCount));
        stats.setFeedbackCount((long) feedbacks.size());
        stats.setAverageScore(averageScore);
        stats.setAverageRating(averageScore);
        stats.setCampusStats(campusStats);
        return stats;
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0 : numerator * 100.0 / denominator;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private String campusOrDefault(String campus) {
        return campus == null || campus.isBlank() ? "未知校区" : campus;
    }
}
