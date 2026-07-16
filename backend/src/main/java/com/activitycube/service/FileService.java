package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.vo.FileUploadResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 100L * 1024 * 1024;

    private final Path uploadRoot;

    public FileService(@Value("${activity-cube.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public FileUploadResult upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extension(originalName);
        String fileType = resolveFileType(extension);
        validateSize(file.getSize(), fileType);

        LocalDate now = LocalDate.now();
        Path relativeDir = Path.of(String.valueOf(now.getYear()), "%02d".formatted(now.getMonthValue()));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetDir = uploadRoot.resolve(relativeDir).normalize();
        Path targetFile = targetDir.resolve(fileName).normalize();
        if (!targetFile.startsWith(uploadRoot)) {
            throw new BusinessException("文件保存路径不合法");
        }
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException exception) {
            throw new BusinessException("文件保存失败，请稍后重试");
        }

        FileUploadResult result = new FileUploadResult();
        result.setUrl("/uploads/%d/%02d/%s".formatted(now.getYear(), now.getMonthValue(), fileName));
        result.setFileName(fileName);
        result.setOriginalName(originalName);
        result.setFileType(fileType);
        result.setSize(file.getSize());
        return result;
    }

    public List<FileUploadResult> uploadBatch(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException("请选择要上传的文件");
        }
        return List.of(files).stream().map(this::upload).toList();
    }

    private String extension(String originalName) {
        int index = originalName.lastIndexOf('.');
        if (index < 0 || index == originalName.length() - 1) {
            throw new BusinessException("仅支持 jpg、jpeg、png、gif、webp、mp4、webm、mov 文件");
        }
        return originalName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String resolveFileType(String extension) {
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return "image";
        }
        if (VIDEO_EXTENSIONS.contains(extension)) {
            return "video";
        }
        throw new BusinessException("仅支持 jpg、jpeg、png、gif、webp、mp4、webm、mov 文件");
    }

    private void validateSize(long size, String fileType) {
        if ("image".equals(fileType) && size > MAX_IMAGE_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }
        if ("video".equals(fileType) && size > MAX_VIDEO_SIZE) {
            throw new BusinessException("视频大小不能超过 100MB");
        }
    }
}
