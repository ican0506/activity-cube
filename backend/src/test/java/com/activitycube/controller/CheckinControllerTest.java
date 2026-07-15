package com.activitycube.controller;

import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.service.CheckinService;
import com.activitycube.util.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckinControllerTest {
    private final CheckinService checkinService = mock(CheckinService.class);
    private final CheckinController controller = new CheckinController(checkinService);

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    @Test
    void absenteesUsesExistingAbsenceList() {
        User user = new User();
        user.setId(2L);
        user.setRole("organizer");
        UserContext.set(user);

        Registration registration = new Registration();
        registration.setId(10L);
        registration.setName("未签到学生");
        when(checkinService.absences(1L, user)).thenReturn(List.of(registration));

        var response = controller.absentees(1L);

        assertThat(response.getData()).containsExactly(registration);
        verify(checkinService).absences(1L, user);
    }
}
