package com.activitycube.service;

import com.activitycube.entity.Checkin;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.Registration;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.vo.ActivityStats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatServiceTest {
    private final ActivityService activityService = mock(ActivityService.class);
    private final RegistrationService registrationService = mock(RegistrationService.class);
    private final CheckinService checkinService = mock(CheckinService.class);
    private final FeedbackMapper feedbackMapper = mock(FeedbackMapper.class);
    private final StatService statService = new StatService(activityService, registrationService, checkinService, feedbackMapper);

    @Test
    void includesConversionRatesForRegistrationCheckinAndFeedback() {
        when(registrationService.registrations(1L)).thenReturn(List.of(
                registration(1L),
                registration(2L),
                registration(3L),
                registration(4L)
        ));
        when(checkinService.checkins(1L)).thenReturn(List.of(
                checkin(1L),
                checkin(2L)
        ));
        when(feedbackMapper.selectList(any())).thenReturn(List.of(feedback(1L)));

        ActivityStats stats = statService.activityStats(1L);

        assertThat(stats.getRegistrationToCheckinRate()).isEqualTo(50.0);
        assertThat(stats.getCheckinToFeedbackRate()).isEqualTo(50.0);
        assertThat(stats.getRegistrationToFeedbackRate()).isEqualTo(25.0);
    }

    private Registration registration(Long id) {
        Registration registration = new Registration();
        registration.setId(id);
        registration.setActivityId(1L);
        registration.setUserId(100L + id);
        registration.setCampus("龙子湖校区");
        return registration;
    }

    private Checkin checkin(Long id) {
        Checkin checkin = new Checkin();
        checkin.setId(id);
        checkin.setActivityId(1L);
        checkin.setUserId(100L + id);
        checkin.setCampus("龙子湖校区");
        return checkin;
    }

    private Feedback feedback(Long id) {
        Feedback feedback = new Feedback();
        feedback.setId(id);
        feedback.setActivityId(1L);
        feedback.setUserId(100L + id);
        feedback.setScore(5);
        return feedback;
    }
}
