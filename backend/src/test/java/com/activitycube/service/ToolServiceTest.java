package com.activitycube.service;

import com.activitycube.dto.GroupRequest;
import com.activitycube.entity.Registration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolServiceTest {
    private final RegistrationService registrationService = mock(RegistrationService.class);
    private final CheckinService checkinService = mock(CheckinService.class);
    private final ToolService toolService = new ToolService(registrationService, checkinService);

    @Test
    void balancesGroupsWhenPeopleCannotBeDividedEvenlyByGroupCount() {
        when(registrationService.registrations(1L)).thenReturn(List.of(
                registration(1L),
                registration(2L),
                registration(3L),
                registration(4L),
                registration(5L),
                registration(6L),
                registration(7L)
        ));
        GroupRequest request = new GroupRequest();
        request.setSource("REGISTRATION");
        request.setMode("BY_GROUP_COUNT");
        request.setGroupCount(3);

        List<List<Registration>> groups = toolService.group(1L, request);

        assertThat(groups).hasSize(3);
        assertThat(groups).extracting(List::size).containsExactly(3, 2, 2);
    }

    private Registration registration(Long id) {
        Registration registration = new Registration();
        registration.setId(id);
        registration.setActivityId(1L);
        registration.setUserId(100L + id);
        registration.setName("同学" + id);
        return registration;
    }
}
