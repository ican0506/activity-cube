package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.service.FileService;
import com.activitycube.vo.FileUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping("/api/files/upload")
    public Result<FileUploadResult> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(fileService.upload(file));
    }

    @PostMapping("/api/files/upload-batch")
    public Result<List<FileUploadResult>> uploadBatch(@RequestParam("files") MultipartFile[] files) {
        return Result.success(fileService.uploadBatch(files));
    }
}
