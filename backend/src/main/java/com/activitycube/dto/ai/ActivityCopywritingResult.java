package com.activitycube.dto.ai;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ActivityCopywritingResult {
    private String title;
    private String summary;
    private List<String> highlights = new ArrayList<>();
    private String socialText;
    private String registrationNotice;
    private LocalDateTime generatedAt;
}
