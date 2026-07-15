package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.LotteryDrawRequest;
import com.activitycube.entity.LotteryResult;
import com.activitycube.entity.Registration;
import com.activitycube.mapper.LotteryResultMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LotteryServiceTest {
    private final RegistrationService registrationService = mock(RegistrationService.class);
    private final CheckinService checkinService = mock(CheckinService.class);
    private final LotteryResultMapper lotteryResultMapper = mock(LotteryResultMapper.class);
    private final LotteryService lotteryService = new LotteryService(registrationService, checkinService, lotteryResultMapper);

    @Test
    void rejectsWhenCountExceedsAvailablePeople() {
        when(registrationService.registrations(1L)).thenReturn(List.of(
                registration(10L, 100L, "张三"),
                registration(11L, 101L, "李四")
        ));

        LotteryDrawRequest request = new LotteryDrawRequest();
        request.setSource("registration");
        request.setCount(3);
        request.setAllowRepeat(true);

        assertThatThrownBy(() -> lotteryService.draw(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("抽取人数不能大于可抽人数");
        verify(lotteryResultMapper, never()).insert(any(LotteryResult.class));
    }

    @Test
    void excludesPreviousWinnersWhenRepeatIsNotAllowed() {
        when(registrationService.registrations(1L)).thenReturn(List.of(
                registration(10L, 100L, "张三"),
                registration(11L, 101L, "李四"),
                registration(12L, 102L, "王五")
        ));
        LotteryResult previous = new LotteryResult();
        previous.setUserId(100L);
        when(lotteryResultMapper.selectList(any())).thenReturn(List.of(previous));

        LotteryDrawRequest request = new LotteryDrawRequest();
        request.setSource("registration");
        request.setCount(2);
        request.setAllowRepeat(false);

        List<LotteryResult> results = lotteryService.draw(1L, request);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(LotteryResult::getUserId).doesNotContain(100L);
        verify(lotteryResultMapper, times(2)).insert(any(LotteryResult.class));
    }

    private Registration registration(Long id, Long userId, String name) {
        Registration registration = new Registration();
        registration.setId(id);
        registration.setActivityId(1L);
        registration.setUserId(userId);
        registration.setName(name);
        registration.setStudentNo("2024" + userId);
        registration.setCollege("信息工程学院");
        registration.setMajorClass("软件工程2401");
        registration.setPhone("1380000" + userId);
        registration.setCampus("龙子湖校区");
        return registration;
    }
}
