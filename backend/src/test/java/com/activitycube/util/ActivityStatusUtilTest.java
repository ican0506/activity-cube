package com.activitycube.util;

import com.activitycube.entity.Activity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityStatusUtilTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 7, 16, 12, 0);

    @Test
    void keepsManualDraftAndCancelledStatus() {
        assertThat(ActivityStatusUtil.calculateStatus(activity("DRAFT"), now)).isEqualTo("DRAFT");
        assertThat(ActivityStatusUtil.calculateStatus(activity("CANCELLED"), now)).isEqualTo("CANCELLED");
    }

    @Test
    void calculatesStatusFromRegistrationAndActivityTime() {
        assertThat(ActivityStatusUtil.calculateStatus(activity("PUBLISHED",
                now.plusHours(1), now.plusHours(2), now.plusHours(3), now.plusHours(4)), now))
                .isEqualTo("NOT_STARTED");

        assertThat(ActivityStatusUtil.calculateStatus(activity("PUBLISHED",
                now.minusHours(1), now.plusHours(1), now.plusHours(2), now.plusHours(3)), now))
                .isEqualTo("REGISTERING");

        assertThat(ActivityStatusUtil.calculateStatus(activity("PUBLISHED",
                now.minusHours(3), now.minusHours(1), now.plusHours(1), now.plusHours(3)), now))
                .isEqualTo("WAITING_START");

        assertThat(ActivityStatusUtil.calculateStatus(activity("PUBLISHED",
                now.minusHours(4), now.minusHours(2), now.minusHours(1), now.plusHours(1)), now))
                .isEqualTo("ONGOING");

        assertThat(ActivityStatusUtil.calculateStatus(activity("PUBLISHED",
                now.minusHours(6), now.minusHours(4), now.minusHours(3), now.minusHours(1)), now))
                .isEqualTo("ENDED");
    }

    private Activity activity(String manualStatus) {
        return activity(manualStatus, now.minusHours(1), now.plusHours(1), now.plusHours(2), now.plusHours(3));
    }

    private Activity activity(String manualStatus,
                              LocalDateTime registerStartTime,
                              LocalDateTime registerEndTime,
                              LocalDateTime startTime,
                              LocalDateTime endTime) {
        Activity activity = new Activity();
        activity.setStatus(manualStatus);
        activity.setRegisterStartTime(registerStartTime);
        activity.setRegisterEndTime(registerEndTime);
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        return activity;
    }
}
