package com.activitycube.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CampusStat {
    private String campus;
    private Long registrationCount;
    private Long checkinCount;
}
