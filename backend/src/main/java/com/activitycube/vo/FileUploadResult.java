package com.activitycube.vo;

import lombok.Data;

@Data
public class FileUploadResult {
    private String url;
    private String fileName;
    private String originalName;
    private String fileType;
    private Long size;
}
