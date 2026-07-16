package com.activitycube.util;

import com.activitycube.entity.Activity;

import java.time.LocalDateTime;

public final class ActivityStatusUtil {
    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String NOT_STARTED = "NOT_STARTED";
    public static final String REGISTERING = "REGISTERING";
    public static final String WAITING_START = "WAITING_START";
    public static final String ONGOING = "ONGOING";
    public static final String ENDED = "ENDED";
    public static final String CANCELLED = "CANCELLED";

    private ActivityStatusUtil() {
    }

    public static String calculateStatus(Activity activity) {
        return calculateStatus(activity, LocalDateTime.now());
    }

    public static String calculateStatus(Activity activity, LocalDateTime now) {
        if (activity == null) {
            return ENDED;
        }
        String manualStatus = activity.getStatus();
        if (DRAFT.equals(manualStatus)) {
            return DRAFT;
        }
        if (CANCELLED.equals(manualStatus)) {
            return CANCELLED;
        }
        if (isBefore(now, activity.getRegisterStartTime())) {
            return NOT_STARTED;
        }
        if (!isBefore(now, activity.getRegisterStartTime()) && !isAfter(now, activity.getRegisterEndTime())) {
            return REGISTERING;
        }
        if (isAfter(now, activity.getRegisterEndTime()) && isBefore(now, activity.getStartTime())) {
            return WAITING_START;
        }
        if (!isBefore(now, activity.getStartTime()) && !isAfter(now, activity.getEndTime())) {
            return ONGOING;
        }
        return ENDED;
    }

    public static void applyCalculatedStatus(Activity activity) {
        activity.setStatus(calculateStatus(activity));
    }

    public static boolean isManualStatus(String status) {
        return DRAFT.equals(status) || CANCELLED.equals(status);
    }

    public static String normalizeManualStatus(String status) {
        if (DRAFT.equals(status) || CANCELLED.equals(status)) {
            return status;
        }
        return PUBLISHED;
    }

    private static boolean isBefore(LocalDateTime left, LocalDateTime right) {
        return left != null && right != null && left.isBefore(right);
    }

    private static boolean isAfter(LocalDateTime left, LocalDateTime right) {
        return left != null && right != null && left.isAfter(right);
    }
}
