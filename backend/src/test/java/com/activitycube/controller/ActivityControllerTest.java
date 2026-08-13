package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.ActivityQueryRequest;
import com.activitycube.entity.Activity;
import com.activitycube.service.ActivityRecommendationService;
import com.activitycube.service.ActivityService;
import com.activitycube.vo.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityControllerTest {
    private final ActivityService activityService = mock(ActivityService.class);
    private final ActivityRecommendationService recommendationService = mock(ActivityRecommendationService.class);
    private final ActivityController controller = new ActivityController(activityService, recommendationService);

    @Test
    void pageDelegatesToActivityServiceWithQueryRequest() {
        ActivityQueryRequest request = new ActivityQueryRequest();
        request.setPageNum(2);
        request.setPageSize(20);
        request.setKeyword("讲座");
        request.setActivityMode("online");
        PageResult<Activity> pageResult = new PageResult<>(List.of(new Activity()), 21, 2, 20);
        when(activityService.page(request)).thenReturn(pageResult);

        Result<PageResult<Activity>> result = controller.page(request);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isSameAs(pageResult);
        verify(activityService).page(same(request));
    }
}
