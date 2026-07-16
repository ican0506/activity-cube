package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.DrawRequest;
import com.activitycube.dto.GroupRequest;
import com.activitycube.entity.Registration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolService {
    private final RegistrationService registrationService;
    private final CheckinService checkinService;

    public List<Registration> draw(Long activityId, DrawRequest request) {
        List<Registration> source = source(activityId, request.getSource());
        if (request.getCount() > source.size()) {
            throw new BusinessException("抽取人数不能超过候选人数");
        }
        List<Registration> shuffled = new ArrayList<>(source);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, request.getCount());
    }

    public List<List<Registration>> group(Long activityId, GroupRequest request) {
        List<Registration> source = new ArrayList<>(source(activityId, request.getSource()));
        Collections.shuffle(source);
        validateGroupRequest(source.size(), request);
        List<List<Registration>> groups = new ArrayList<>();
        if ("BY_GROUP_COUNT".equalsIgnoreCase(request.getMode())) {
            int groupCount = Math.min(request.getGroupCount(), source.size());
            int baseSize = source.size() / groupCount;
            int remainder = source.size() % groupCount;
            int index = 0;
            for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
                int currentSize = baseSize + (groupIndex < remainder ? 1 : 0);
                groups.add(source.subList(index, index + currentSize));
                index += currentSize;
            }
            return groups;
        }
        int groupSize = request.getGroupSize();
        for (int index = 0; index < source.size(); index += groupSize) {
            groups.add(source.subList(index, Math.min(index + groupSize, source.size())));
        }
        return groups;
    }

    private List<Registration> source(Long activityId, String source) {
        if ("CHECKIN".equalsIgnoreCase(source)) {
            var checkedUserIds = checkinService.checkins(activityId).stream()
                    .map(checkin -> checkin.getUserId())
                    .toList();
            return registrationService.registrations(activityId).stream()
                    .filter(registration -> checkedUserIds.contains(registration.getUserId()))
                    .toList();
        }
        if ("REGISTRATION".equalsIgnoreCase(source)) {
            return registrationService.registrations(activityId);
        }
        throw new BusinessException("抽取来源不正确");
    }

    private void validateGroupRequest(int total, GroupRequest request) {
        if (total == 0) {
            throw new BusinessException("候选名单为空");
        }
        if ("BY_GROUP_COUNT".equalsIgnoreCase(request.getMode())) {
            if (request.getGroupCount() == null || request.getGroupCount() <= 0) {
                throw new BusinessException("组数必须大于 0");
            }
            return;
        }
        if ("BY_GROUP_SIZE".equalsIgnoreCase(request.getMode())) {
            if (request.getGroupSize() == null || request.getGroupSize() <= 0) {
                throw new BusinessException("每组人数必须大于 0");
            }
            return;
        }
        throw new BusinessException("分组模式不正确");
    }
}
