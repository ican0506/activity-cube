package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.ActivityQueryRequest;
import com.activitycube.dto.ActivityRequest;
import com.activitycube.dto.RejectActivityRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.util.ActivityStatusUtil;
import com.activitycube.util.AuthUtil;
import com.activitycube.util.UserContext;
import com.activitycube.vo.ActivityCountVO;
import com.activitycube.vo.ActivityDetail;
import com.activitycube.vo.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {
    public static final String MODE_ONLINE = "online";
    public static final String MODE_OFFLINE = "offline";
    public static final String MODE_HYBRID = "hybrid";
    public static final String CHECKIN_MODE_ONLINE = "online";
    public static final String CHECKIN_MODE_QR = "qr";
    public static final String CHECKIN_MODE_BOTH = "both";

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final CheckinMapper checkinMapper;
    private final FeedbackMapper feedbackMapper;
    private final OperationLogService operationLogService;
    private final NoticeService noticeService;

    public List<Activity> list(String keyword, String campus, String status) {
        User currentUser = UserContext.get().orElse(null);
        LambdaQueryWrapper<Activity> wrapper = baseListWrapper(keyword, campus, currentUser)
                .orderByDesc(Activity::getCreatedAt);
        List<Activity> activities = activityMapper.selectList(wrapper).stream()
                .peek(this::applyResponseDefaults)
                .filter(activity -> !StringUtils.hasText(status)
                        || "全部".equals(status)
                        || status.equals(activity.getStatus()))
                .toList();
        if (activities.isEmpty()) {
            return List.of();
        }
        enrichActivities(activities, currentUser);
        return activities;
    }

    public PageResult<Activity> page(ActivityQueryRequest request) {
        ActivityQueryRequest query = request == null ? new ActivityQueryRequest() : request;
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        LocalDateTime now = LocalDateTime.now();
        User currentUser = UserContext.get().orElse(null);
        LambdaQueryWrapper<Activity> wrapper = baseListWrapper(query.getKeyword(), query.getCampus(), currentUser);
        applyStatusCondition(wrapper, query.getStatus(), now);
        wrapper.orderByDesc(Activity::getCreatedAt).orderByDesc(Activity::getId);

        Page<Activity> result = activityMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Activity> records = result.getRecords();
        if (records == null || records.isEmpty()) {
            return new PageResult<>(List.of(), result.getTotal(), pageNum, pageSize);
        }
        records.forEach(activity -> applyResponseDefaults(activity, now));
        enrichActivities(records, currentUser);
        return new PageResult<>(records, result.getTotal(), pageNum, pageSize);
    }

    public ActivityDetail detail(Long id) {
        Activity activity = requireActivity(id);
        if (!canViewActivity(activity, UserContext.get().orElse(null))) {
            throw new BusinessException("活动尚未发布");
        }
        User currentUser = UserContext.get().orElse(null);
        applyStudentResponse(activity, currentUser);
        ActivityDetail detail = new ActivityDetail();
        detail.setActivity(activity);
        detail.setRegistrationCount(activity.getRegistrationCount());
        detail.setCheckinCount(activity.getCheckinCount());
        detail.setRegistered(Boolean.TRUE.equals(activity.getRegistered()));
        detail.setCheckedIn(Boolean.TRUE.equals(activity.getCheckedIn()));
        return detail;
    }

    public Activity create(ActivityRequest request, User creator) {
        AuthUtil.requireOrganizerOrAdmin(creator);
        Activity activity = new Activity();
        BeanUtils.copyProperties(request, activity);
        activity.setStatus(ActivityStatusUtil.DRAFT);
        activity.setActivityMode(normalizeActivityMode(request.getActivityMode()));
        activity.setCheckinMode(normalizeCheckinMode(request.getCheckinMode(), activity.getActivityMode()));
        applyActivityDefaults(activity);
        activity.setCreatorId(creator.getId());
        activity.setCreatedAt(LocalDateTime.now());
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.insert(activity);
        applyResponseDefaults(activity);
        return activity;
    }

    @Transactional
    public Activity update(Long id, ActivityRequest request, User user) {
        Activity activity = requireManageableActivity(id, user);
        applyActivityDefaults(activity);
        if (ActivityStatusUtil.CANCELLED.equals(activity.getStatus())) {
            throw new BusinessException("活动已取消，不能继续编辑");
        }
        String workflowStatus = activity.getStatus();
        String calculatedStatus = ActivityStatusUtil.calculateStatus(activity);
        if (ActivityStatusUtil.ENDED.equals(calculatedStatus) && !"admin".equals(user.getRole())) {
            throw new BusinessException("活动已结束，不能继续编辑");
        }
        Activity before = copyActivity(activity);
        boolean publishedWorkflow = ActivityStatusUtil.PUBLISHED.equals(workflowStatus);
        BeanUtils.copyProperties(request, activity);
        activity.setStatus(workflowStatus);
        activity.setCreatorId(before.getCreatorId());
        activity.setCheckinCode(before.getCheckinCode());
        activity.setRejectReason(before.getRejectReason());
        if (publishedWorkflow && !"admin".equals(user.getRole())) {
            restoreOrganizerPublishedCoreFields(activity, before);
        }
        activity.setActivityMode(normalizeActivityMode(request.getActivityMode()));
        if (publishedWorkflow && !"admin".equals(user.getRole())) {
            activity.setActivityMode(before.getActivityMode());
        }
        activity.setCheckinMode(normalizeCheckinMode(request.getCheckinMode(), activity.getActivityMode()));
        if (publishedWorkflow && !"admin".equals(user.getRole())
                && isOngoingCalculatedStatus(calculatedStatus)
                && !Objects.equals(before.getCheckinMode(), activity.getCheckinMode())) {
            activity.setCheckinMode(before.getCheckinMode());
        }
        applyActivityDefaults(activity);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        if (publishedWorkflow) {
            operationLogService.record(user, "update_published_activity", "activity", id, "更新已发布活动：" + activity.getTitle());
            List<String> changedFields = changedPublishedKeyFields(before, activity);
            if (!changedFields.isEmpty() && !ActivityStatusUtil.ENDED.equals(calculatedStatus)) {
                noticeService.notifyActivityUpdated(activity, user, changedFields);
            }
        }
        applyResponseDefaults(activity);
        return activity;
    }

    public void delete(Long id, User user) {
        requireManageableActivity(id, user);
        activityMapper.deleteById(id);
    }

    public Activity submitReview(Long id, User user) {
        Activity activity = requireManageableActivity(id, user);
        if (!ActivityStatusUtil.DRAFT.equals(activity.getStatus()) && !ActivityStatusUtil.REJECTED.equals(activity.getStatus())) {
            throw new BusinessException("当前活动不能提交审核");
        }
        activity.setStatus(ActivityStatusUtil.PENDING_REVIEW);
        activity.setRejectReason(null);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(user, "submit_activity_review", "activity", id, "提交活动审核：" + activity.getTitle());
        applyResponseDefaults(activity);
        return activity;
    }

    public List<Activity> pendingReviews(User user) {
        AuthUtil.requireAdmin(user);
        return activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getStatus, ActivityStatusUtil.PENDING_REVIEW)
                        .orderByAsc(Activity::getUpdatedAt))
                .stream()
                .peek(this::applyResponseDefaults)
                .toList();
    }

    public Activity approveReview(Long id, User user) {
        AuthUtil.requireAdmin(user);
        Activity activity = requireStoredActivity(id);
        if (!ActivityStatusUtil.PENDING_REVIEW.equals(activity.getStatus())) {
            throw new BusinessException("该活动不处于待审核状态");
        }
        activity.setStatus(ActivityStatusUtil.PUBLISHED);
        activity.setRejectReason(null);
        ensureCheckinCodeIfPublished(activity);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(user, "approve_activity", "activity", id, "审核通过活动：" + activity.getTitle());
        applyResponseDefaults(activity);
        return activity;
    }

    public Activity rejectReview(Long id, RejectActivityRequest request, User user) {
        AuthUtil.requireAdmin(user);
        Activity activity = requireStoredActivity(id);
        if (!ActivityStatusUtil.PENDING_REVIEW.equals(activity.getStatus())) {
            throw new BusinessException("该活动不处于待审核状态");
        }
        activity.setStatus(ActivityStatusUtil.REJECTED);
        activity.setRejectReason(request.getReason().trim());
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(user, "reject_activity", "activity", id, "驳回活动：" + activity.getTitle() + "；原因：" + activity.getRejectReason());
        applyResponseDefaults(activity);
        return activity;
    }

    @Transactional
    public Activity cancel(Long id, User user) {
        Activity activity = requireManageableActivity(id, user);
        if (ActivityStatusUtil.ENDED.equals(ActivityStatusUtil.calculateStatus(activity))) {
            throw new BusinessException("活动已结束，不能取消");
        }
        activity.setStatus(ActivityStatusUtil.CANCELLED);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(user, "cancel_activity", "activity", id, "取消活动：" + activity.getTitle());
        noticeService.notifyActivityCancelled(activity, user);
        applyResponseDefaults(activity);
        return activity;
    }

    public Activity qrCodeActivity(Long id, User user) {
        return requireManageableActivity(id, user);
    }

    public String generateCheckinCode(Long id, User user) {
        Activity activity = requireManageableActivity(id, user);
        activity.setCheckinCode(UUID.randomUUID().toString().replace("-", ""));
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(user, "refresh_checkin_qr", "activity", id, "刷新签到二维码：" + activity.getTitle());
        return activity.getCheckinCode();
    }

    public Activity requireActivity(Long id) {
        Activity activity = requireStoredActivity(id);
        applyResponseDefaults(activity);
        return activity;
    }

    private Activity requireStoredActivity(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    public Activity requireManageableActivity(Long id, User user) {
        AuthUtil.requireOrganizerOrAdmin(user);
        Activity activity = requireStoredActivity(id);
        if (!"admin".equals(user.getRole()) && !Objects.equals(activity.getCreatorId(), user.getId())) {
            throw new BusinessException("只能管理自己创建的活动");
        }
        return activity;
    }

    public Long countRegistrations(Long activityId) {
        return registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId));
    }

    public Long countCheckins(Long activityId) {
        return checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activityId));
    }

    private boolean hasRegistration(Long activityId, Long userId) {
        return registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getUserId, userId)) > 0;
    }

    private boolean hasCheckin(Long activityId, Long userId) {
        return checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activityId)
                .eq(Checkin::getUserId, userId)) > 0;
    }

    private boolean hasFeedback(Long activityId, Long userId) {
        return feedbackMapper.selectCount(new LambdaQueryWrapper<com.activitycube.entity.Feedback>()
                .eq(com.activitycube.entity.Feedback::getActivityId, activityId)
                .eq(com.activitycube.entity.Feedback::getUserId, userId)) > 0;
    }

    private String normalizeActivityMode(String activityMode) {
        if (MODE_ONLINE.equalsIgnoreCase(activityMode)) {
            return MODE_ONLINE;
        }
        if (MODE_HYBRID.equalsIgnoreCase(activityMode)) {
            return MODE_HYBRID;
        }
        return MODE_OFFLINE;
    }

    private Activity copyActivity(Activity source) {
        Activity target = new Activity();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private void restoreOrganizerPublishedCoreFields(Activity activity, Activity before) {
        activity.setTitle(before.getTitle());
        activity.setActivityMode(before.getActivityMode());
        activity.setActivityCategory(before.getActivityCategory());
        activity.setCampus(before.getCampus());
        activity.setRegisterStartTime(before.getRegisterStartTime());
        activity.setAllowCrossCampus(before.getAllowCrossCampus());
        activity.setRewardEnabled(before.getRewardEnabled());
        activity.setRewardType(before.getRewardType());
        activity.setRewardHours(before.getRewardHours());
        activity.setRewardPoints(before.getRewardPoints());
        activity.setRewardDescription(before.getRewardDescription());
    }

    private List<String> changedPublishedKeyFields(Activity before, Activity after) {
        List<String> fields = new ArrayList<>();
        if (!Objects.equals(before.getStartTime(), after.getStartTime()) || !Objects.equals(before.getEndTime(), after.getEndTime())) {
            fields.add("活动时间");
        }
        if (!Objects.equals(before.getLocation(), after.getLocation())) {
            fields.add("活动地点");
        }
        if (!Objects.equals(before.getRegisterEndTime(), after.getRegisterEndTime())) {
            fields.add("报名截止时间");
        }
        if (!Objects.equals(before.getCheckinStartTime(), after.getCheckinStartTime())
                || !Objects.equals(before.getCheckinEndTime(), after.getCheckinEndTime())) {
            fields.add("签到时间");
        }
        if (!Objects.equals(before.getCheckinMode(), after.getCheckinMode())) {
            fields.add("签到方式");
        }
        if (!Objects.equals(before.getMaxParticipants(), after.getMaxParticipants())) {
            fields.add("人数上限");
        }
        return fields;
    }

    private void ensureCheckinCodeIfPublished(Activity activity) {
        if (!ActivityStatusUtil.PUBLISHED.equals(activity.getStatus())) {
            return;
        }
        if (!StringUtils.hasText(activity.getCheckinCode())) {
            activity.setCheckinCode(UUID.randomUUID().toString().replace("-", ""));
        }
    }

    private void applyResponseDefaults(Activity activity) {
        activity.setReviewStatus(activity.getStatus());
        ActivityStatusUtil.applyCalculatedStatus(activity);
        applyActivityDefaults(activity);
    }

    private void applyResponseDefaults(Activity activity, LocalDateTime now) {
        activity.setReviewStatus(activity.getStatus());
        activity.setStatus(ActivityStatusUtil.calculateStatus(activity, now));
        applyActivityDefaults(activity);
    }

    private void applyStudentResponse(Activity activity, User user) {
        applyResponseDefaults(activity);
        Long registrationCount = countRegistrations(activity.getId());
        Long checkinCount = countCheckins(activity.getId());
        activity.setRegistrationCount(registrationCount);
        activity.setCheckinCount(checkinCount);

        boolean registered = false;
        boolean checkedIn = false;
        boolean feedbackSubmitted = false;
        if (user != null && user.getId() != null && ("student".equals(user.getRole()) || "user".equals(user.getRole()))) {
            registered = hasRegistration(activity.getId(), user.getId());
            checkedIn = hasCheckin(activity.getId(), user.getId());
            feedbackSubmitted = hasFeedback(activity.getId(), user.getId());
        }

        boolean full = activity.getMaxParticipants() != null
                && activity.getMaxParticipants() > 0
                && registrationCount >= activity.getMaxParticipants();
        boolean canRegister = ActivityStatusUtil.REGISTERING.equals(activity.getStatus()) && !registered && !full;
        boolean canCheckin = registered && !checkedIn && isWithinCheckinWindow(activity);
        boolean canOnlineCheckin = canCheckin && supportsOnlineCheckin(activity);
        boolean canQrCheckin = canCheckin && supportsQrCheckin(activity);

        activity.setRegistered(registered);
        activity.setCheckedIn(checkedIn);
        activity.setFeedbackSubmitted(feedbackSubmitted);
        activity.setCanRegister(canRegister);
        activity.setCanCheckin(canCheckin);
        activity.setCanOnlineCheckin(canOnlineCheckin);
        activity.setCanQrCheckin(canQrCheckin);
        activity.setStudentActivityStatusText(resolveStudentActivityStatusText(activity, full));
    }

    private ActivityListContext prepareActivityListContext(List<Activity> activities, User user) {
        List<Long> activityIds = activities.stream()
                .map(Activity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (activityIds.isEmpty()) {
            return ActivityListContext.empty(user);
        }
        Map<Long, Long> registrationCountMap = toCountMap(registrationMapper.countByActivityIds(activityIds));
        Map<Long, Long> checkinCountMap = toCountMap(checkinMapper.countByActivityIds(activityIds));
        if (!isStudent(user)) {
            return new ActivityListContext(registrationCountMap, checkinCountMap, Set.of(), Set.of(), Set.of(), false);
        }
        Set<Long> registeredActivityIds = toSet(registrationMapper.findActivityIdsByUserAndActivityIds(user.getId(), activityIds));
        Set<Long> checkedInActivityIds = toSet(checkinMapper.findActivityIdsByUserAndActivityIds(user.getId(), activityIds));
        Set<Long> feedbackActivityIds = toSet(feedbackMapper.findActivityIdsByUserAndActivityIds(user.getId(), activityIds));
        return new ActivityListContext(registrationCountMap, checkinCountMap, registeredActivityIds, checkedInActivityIds, feedbackActivityIds, true);
    }

    private void enrichActivities(List<Activity> activities, User currentUser) {
        if (activities == null || activities.isEmpty()) {
            return;
        }
        ActivityListContext context = prepareActivityListContext(activities, currentUser);
        activities.forEach(activity -> applyStudentResponseFromContext(activity, context));
    }

    private void applyStudentResponseFromContext(Activity activity, ActivityListContext context) {
        Long activityId = activity.getId();
        Long registrationCount = context.registrationCountMap().getOrDefault(activityId, 0L);
        Long checkinCount = context.checkinCountMap().getOrDefault(activityId, 0L);
        boolean registered = context.student() && context.registeredActivityIds().contains(activityId);
        boolean checkedIn = context.student() && context.checkedInActivityIds().contains(activityId);
        boolean feedbackSubmitted = context.student() && context.feedbackActivityIds().contains(activityId);
        boolean full = activity.getMaxParticipants() != null
                && activity.getMaxParticipants() > 0
                && registrationCount >= activity.getMaxParticipants();
        boolean canRegister = ActivityStatusUtil.REGISTERING.equals(activity.getStatus()) && !registered && !full;
        boolean canCheckin = registered && !checkedIn && isWithinCheckinWindow(activity);
        boolean canOnlineCheckin = canCheckin && supportsOnlineCheckin(activity);
        boolean canQrCheckin = canCheckin && supportsQrCheckin(activity);

        activity.setRegistrationCount(registrationCount);
        activity.setCheckinCount(checkinCount);
        activity.setRegistered(registered);
        activity.setCheckedIn(checkedIn);
        activity.setFeedbackSubmitted(feedbackSubmitted);
        activity.setCanRegister(canRegister);
        activity.setCanCheckin(canCheckin);
        activity.setCanOnlineCheckin(canOnlineCheckin);
        activity.setCanQrCheckin(canQrCheckin);
        activity.setStudentActivityStatusText(resolveStudentActivityStatusText(activity, full));
    }

    private Map<Long, Long> toCountMap(List<ActivityCountVO> counts) {
        if (counts == null || counts.isEmpty()) {
            return Map.of();
        }
        return counts.stream()
                .filter(count -> count.getActivityId() != null)
                .collect(Collectors.toMap(
                        ActivityCountVO::getActivityId,
                        count -> count.getCount() == null ? 0L : count.getCount(),
                        Long::sum
                ));
    }

    private Set<Long> toSet(List<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Set.of();
        }
        return activityIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean isStudent(User user) {
        return user != null && user.getId() != null && ("student".equals(user.getRole()) || "user".equals(user.getRole()));
    }

    private LambdaQueryWrapper<Activity> baseListWrapper(String keyword, String campus, User currentUser) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        if (!isManager(currentUser)) {
            wrapper.eq(Activity::getStatus, ActivityStatusUtil.PUBLISHED);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Activity::getTitle, keyword);
        }
        if (StringUtils.hasText(campus) && !"全部".equals(campus) && !"ALL".equalsIgnoreCase(campus)) {
            wrapper.eq(Activity::getCampus, campus);
        }
        return wrapper;
    }

    private void applyStatusCondition(LambdaQueryWrapper<Activity> wrapper, String status, LocalDateTime now) {
        if (!StringUtils.hasText(status) || "全部".equals(status) || "ALL".equalsIgnoreCase(status)) {
            return;
        }
        if (ActivityStatusUtil.DRAFT.equals(status)
                || ActivityStatusUtil.PENDING_REVIEW.equals(status)
                || ActivityStatusUtil.REJECTED.equals(status)
                || ActivityStatusUtil.CANCELLED.equals(status)
                || ActivityStatusUtil.PUBLISHED.equals(status)) {
            wrapper.eq(Activity::getStatus, status);
            return;
        }
        if (ActivityStatusUtil.NOT_STARTED.equals(status)) {
            wrapper.eq(Activity::getStatus, ActivityStatusUtil.PUBLISHED)
                    .gt(Activity::getRegisterStartTime, now);
            return;
        }
        if (ActivityStatusUtil.REGISTERING.equals(status)) {
            wrapper.eq(Activity::getStatus, ActivityStatusUtil.PUBLISHED)
                    .le(Activity::getRegisterStartTime, now)
                    .ge(Activity::getRegisterEndTime, now);
            return;
        }
        if (ActivityStatusUtil.WAITING_START.equals(status)) {
            wrapper.eq(Activity::getStatus, ActivityStatusUtil.PUBLISHED)
                    .lt(Activity::getRegisterEndTime, now)
                    .gt(Activity::getStartTime, now);
            return;
        }
        if (ActivityStatusUtil.ONGOING.equals(status)) {
            wrapper.eq(Activity::getStatus, ActivityStatusUtil.PUBLISHED)
                    .le(Activity::getStartTime, now)
                    .ge(Activity::getEndTime, now);
            return;
        }
        if (ActivityStatusUtil.ENDED.equals(status)) {
            wrapper.eq(Activity::getStatus, ActivityStatusUtil.PUBLISHED)
                    .lt(Activity::getEndTime, now);
            return;
        }
        wrapper.eq(Activity::getStatus, status);
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 50);
    }

    private boolean isWithinCheckinWindow(Activity activity) {
        if (ActivityStatusUtil.DRAFT.equals(activity.getStatus())
                || ActivityStatusUtil.PENDING_REVIEW.equals(activity.getStatus())
                || ActivityStatusUtil.REJECTED.equals(activity.getStatus())
                || ActivityStatusUtil.CANCELLED.equals(activity.getStatus())
                || ActivityStatusUtil.ENDED.equals(activity.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = activity.getCheckinStartTime() == null ? activity.getStartTime() : activity.getCheckinStartTime();
        LocalDateTime end = activity.getCheckinEndTime() == null ? activity.getEndTime() : activity.getCheckinEndTime();
        return start != null && end != null && !now.isBefore(start) && !now.isAfter(end);
    }

    private String resolveStudentActivityStatusText(Activity activity, boolean full) {
        if (ActivityStatusUtil.CANCELLED.equals(activity.getStatus())) {
            return "已取消";
        }
        if (ActivityStatusUtil.ENDED.equals(activity.getStatus())) {
            if (Boolean.TRUE.equals(activity.getCheckedIn()) && !Boolean.TRUE.equals(activity.getFeedbackSubmitted())) {
                return "去反馈";
            }
            if (Boolean.TRUE.equals(activity.getCheckedIn()) && Boolean.TRUE.equals(activity.getFeedbackSubmitted())) {
                return "已完成";
            }
            return "已结束";
        }
        if (Boolean.TRUE.equals(activity.getCheckedIn())) {
            return "已签到";
        }
        if (Boolean.TRUE.equals(activity.getRegistered()) && Boolean.TRUE.equals(activity.getCanCheckin())) {
            return "去签到";
        }
        if (Boolean.TRUE.equals(activity.getRegistered())) {
            return "已报名";
        }
        if (full) {
            return "名额已满";
        }
        if (Boolean.TRUE.equals(activity.getCanRegister())) {
            return "立即报名";
        }
        return statusText(activity.getStatus());
    }

    private String statusText(String status) {
        if (ActivityStatusUtil.DRAFT.equals(status)) {
            return "草稿";
        }
        if (ActivityStatusUtil.PENDING_REVIEW.equals(status)) {
            return "待审核";
        }
        if (ActivityStatusUtil.REJECTED.equals(status)) {
            return "已驳回";
        }
        if (ActivityStatusUtil.NOT_STARTED.equals(status)) {
            return "未开始";
        }
        if (ActivityStatusUtil.REGISTERING.equals(status)) {
            return "报名中";
        }
        if (ActivityStatusUtil.WAITING_START.equals(status)) {
            return "待开始";
        }
        if (ActivityStatusUtil.ONGOING.equals(status)) {
            return "进行中";
        }
        if (ActivityStatusUtil.ENDED.equals(status)) {
            return "已结束";
        }
        if (ActivityStatusUtil.CANCELLED.equals(status)) {
            return "已取消";
        }
        return status == null ? "-" : status;
    }

    private void applyActivityDefaults(Activity activity) {
        if (!StringUtils.hasText(activity.getActivityMode())) {
            activity.setActivityMode(MODE_OFFLINE);
        }
        activity.setActivityMode(normalizeActivityMode(activity.getActivityMode()));
        activity.setCheckinMode(normalizeCheckinMode(activity.getCheckinMode(), activity.getActivityMode()));
        if (!StringUtils.hasText(activity.getActivityCategory())) {
            activity.setActivityCategory("其他");
        }
        if (activity.getRewardEnabled() == null) {
            activity.setRewardEnabled(false);
        }
        if (!StringUtils.hasText(activity.getRewardType())) {
            activity.setRewardType("无");
        }
    }

    private boolean isManager(User user) {
        return user != null && ("organizer".equals(user.getRole()) || "admin".equals(user.getRole()));
    }

    private record ActivityListContext(Map<Long, Long> registrationCountMap,
                                       Map<Long, Long> checkinCountMap,
                                       Set<Long> registeredActivityIds,
                                       Set<Long> checkedInActivityIds,
                                       Set<Long> feedbackActivityIds,
                                       boolean student) {
        static ActivityListContext empty(User user) {
            boolean student = user != null && user.getId() != null && ("student".equals(user.getRole()) || "user".equals(user.getRole()));
            return new ActivityListContext(Map.of(), Map.of(), Set.of(), Set.of(), Set.of(), student);
        }
    }

    private String normalizeCheckinMode(String checkinMode, String activityMode) {
        if (CHECKIN_MODE_QR.equalsIgnoreCase(checkinMode)) {
            return CHECKIN_MODE_QR;
        }
        if (CHECKIN_MODE_BOTH.equalsIgnoreCase(checkinMode)) {
            return CHECKIN_MODE_BOTH;
        }
        if (CHECKIN_MODE_ONLINE.equalsIgnoreCase(checkinMode)) {
            return CHECKIN_MODE_ONLINE;
        }
        return defaultCheckinMode(activityMode);
    }

    public static String defaultCheckinMode(String activityMode) {
        if (MODE_OFFLINE.equals(activityMode)) {
            return CHECKIN_MODE_QR;
        }
        return CHECKIN_MODE_ONLINE;
    }

    public static boolean supportsOnlineCheckin(Activity activity) {
        String mode = StringUtils.hasText(activity.getCheckinMode())
                ? activity.getCheckinMode()
                : defaultCheckinMode(activity.getActivityMode());
        return CHECKIN_MODE_ONLINE.equals(mode) || CHECKIN_MODE_BOTH.equals(mode);
    }

    public static boolean supportsQrCheckin(Activity activity) {
        String mode = StringUtils.hasText(activity.getCheckinMode())
                ? activity.getCheckinMode()
                : defaultCheckinMode(activity.getActivityMode());
        return CHECKIN_MODE_QR.equals(mode) || CHECKIN_MODE_BOTH.equals(mode);
    }

    private boolean isOngoingCalculatedStatus(String status) {
        return ActivityStatusUtil.ONGOING.equals(status);
    }

    private boolean canViewActivity(Activity activity, User user) {
        if (ActivityStatusUtil.PUBLISHED.equals(activity.getReviewStatus())) {
            return true;
        }
        return user != null && ("admin".equals(user.getRole())
                || ("organizer".equals(user.getRole()) && Objects.equals(activity.getCreatorId(), user.getId())));
    }
}
