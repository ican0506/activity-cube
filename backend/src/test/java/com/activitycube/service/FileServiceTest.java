package com.activitycube.service;

import com.activitycube.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileServiceTest {
    @TempDir
    Path uploadRoot;

    @Test
    void storesImageWithGeneratedFileNameAndRelativeUrl() throws Exception {
        FileService fileService = new FileService(uploadRoot.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "活动照片.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        var result = fileService.upload(file);

        LocalDate now = LocalDate.now();
        assertThat(result.getUrl()).startsWith("/uploads/%04d/%02d/".formatted(now.getYear(), now.getMonthValue()));
        assertThat(result.getFileName()).endsWith(".jpg");
        assertThat(result.getOriginalName()).isEqualTo("活动照片.jpg");
        assertThat(result.getFileType()).isEqualTo("image");
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(Files.exists(uploadRoot.resolve(result.getUrl().replace("/uploads/", "").replace("/", java.io.File.separator))))
                .isTrue();
    }

    @Test
    void rejectsUnsupportedFileType() {
        FileService fileService = new FileService(uploadRoot.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> fileService.upload(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持");
    }

    @Test
    void storesAvatarUnderAvatarDirectory() throws Exception {
        FileService fileService = new FileService(uploadRoot.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "头像.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        var result = fileService.uploadAvatar(file);

        assertThat(result.getUrl()).startsWith("/uploads/avatar/");
        assertThat(result.getFileName()).endsWith(".png");
        assertThat(result.getFileType()).isEqualTo("image");
        assertThat(Files.exists(uploadRoot.resolve(result.getUrl().replace("/uploads/", "").replace("/", java.io.File.separator))))
                .isTrue();
    }

    @Test
    void rejectsGifAvatar() {
        FileService fileService = new FileService(uploadRoot.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.gif",
                "image/gif",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> fileService.uploadAvatar(file))
                .isInstanceOf(BusinessException.class)
                .hasMessage("头像只支持 jpg、jpeg、png、webp 图片");
    }
}
