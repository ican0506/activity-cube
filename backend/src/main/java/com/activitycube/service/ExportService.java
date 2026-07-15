package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.entity.Registration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {
    private final RegistrationService registrationService;
    private final CheckinService checkinService;

    public byte[] export(Long activityId, String type) {
        List<Registration> registrations = switch (type) {
            case "registrations" -> registrationService.registrations(activityId);
            case "checkins" -> checkedRegistrations(activityId);
            case "absences" -> absentRegistrations(activityId);
            default -> throw new BusinessException("导出类型不正确");
        };
        StringBuilder builder = new StringBuilder("\uFEFF姓名,学号,学院,专业班级,手机号,校区,备注\n");
        for (Registration item : registrations) {
            builder.append(csv(item.getName())).append(',')
                    .append(csv(item.getStudentNo())).append(',')
                    .append(csv(item.getCollege())).append(',')
                    .append(csv(item.getMajorClass())).append(',')
                    .append(csv(item.getPhone())).append(',')
                    .append(csv(item.getCampus())).append(',')
                    .append(csv(item.getRemark())).append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<Registration> checkedRegistrations(Long activityId) {
        Set<Long> checkedUserIds = checkinService.checkins(activityId).stream()
                .map(checkin -> checkin.getUserId())
                .collect(Collectors.toSet());
        return registrationService.registrations(activityId).stream()
                .filter(registration -> checkedUserIds.contains(registration.getUserId()))
                .toList();
    }

    private List<Registration> absentRegistrations(Long activityId) {
        Set<Long> checkedUserIds = checkinService.checkins(activityId).stream()
                .map(checkin -> checkin.getUserId())
                .collect(Collectors.toSet());
        return registrationService.registrations(activityId).stream()
                .filter(registration -> !checkedUserIds.contains(registration.getUserId()))
                .toList();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
