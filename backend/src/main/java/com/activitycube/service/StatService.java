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

import java.util.List;
import java.util.Map;
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
        List<Registration> registrations = registrationService.registrations(activityId);
        List<Checkin> checkins = checkinService.checkins(activityId);
        List<Feedback> feedbacks = feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getActivityId, activityId));

        long registrationCount = registrations.size();
        long checkinCount = checkins.size();

        Map<String, Long> registrationByCampus = registrations.stream()
                .collect(Collectors.groupingBy(Registration::getCampus, Collectors.counting()));
        Map<String, Long> checkinByCampus = checkins.stream()
                .collect(Collectors.groupingBy(Checkin::getCampus, Collectors.counting()));

        List<CampusStat> campusStats = registrationByCampus.keySet().stream()
                .map(campus -> new CampusStat(campus, registrationByCampus.getOrDefault(campus, 0L), checkinByCampus.getOrDefault(campus, 0L)))
                .toList();

        ActivityStats stats = new ActivityStats();
        stats.setActivityId(activityId);
        stats.setRegistrationCount(registrationCount);
        stats.setCheckinCount(checkinCount);
        stats.setAbsenceCount(registrationCount - checkinCount);
        stats.setCheckinRate(rate(checkinCount, registrationCount));
        stats.setRegistrationToCheckinRate(rate(checkinCount, registrationCount));
        stats.setCheckinToFeedbackRate(rate(feedbacks.size(), checkinCount));
        stats.setRegistrationToFeedbackRate(rate(feedbacks.size(), registrationCount));
        stats.setFeedbackCount((long) feedbacks.size());
        stats.setAverageRating(feedbacks.isEmpty() ? 0 : feedbacks.stream().mapToInt(Feedback::getScore).average().orElse(0));
        stats.setCampusStats(campusStats);
        return stats;
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0 : numerator * 100.0 / denominator;
    }
}
