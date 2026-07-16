package com.activitycube.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FeedbackMediaRequest {
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private String mediaType;
        private String url;
        private String fileName;
        private String originalName;
        private Long size;
    }
}
