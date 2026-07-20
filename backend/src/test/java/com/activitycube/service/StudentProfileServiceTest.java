package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.StudentPasswordRequest;
import com.activitycube.dto.StudentProfileUpdateRequest;
import com.activitycube.entity.StudentActivityReward;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.NoticeReceiverMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.mapper.StudentActivityRewardMapper;
import com.activitycube.mapper.UserMapper;
import com.activitycube.vo.FileUploadResult;
import com.activitycube.vo.StudentProfileSummary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentProfileServiceTest {
    private final UserMapper userMapper = mock(UserMapper.class);
    private final RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
    private final CheckinMapper checkinMapper = mock(CheckinMapper.class);
    private final FeedbackMapper feedbackMapper = mock(FeedbackMapper.class);
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final StudentActivityRewardMapper rewardMapper = mock(StudentActivityRewardMapper.class);
    private final NoticeReceiverMapper noticeReceiverMapper = mock(NoticeReceiverMapper.class);
    private final FileService fileService = mock(FileService.class);
    private final PasswordService passwordService = new PasswordService();
    private final StudentProfileService profileService = new StudentProfileService(
            userMapper,
            registrationMapper,
            checkinMapper,
            feedbackMapper,
            activityMapper,
            rewardMapper,
            noticeReceiverMapper,
            fileService,
            passwordService);

    @Test
    void rejectsNonStudentProfileAccess() {
        User organizer = new User();
        organizer.setRole("organizer");

        assertThatThrownBy(() -> profileService.profile(organizer))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有学生可以访问个人中心");
    }

    @Test
    void updatesOnlyEditableProfileFields() {
        User student = student();
        StudentProfileUpdateRequest request = new StudentProfileUpdateRequest();
        request.setAvatarUrl("/uploads/avatar.jpg");
        request.setPhone("13800001111");
        request.setBio("热爱校园公益活动");
        when(userMapper.selectById(3L)).thenReturn(student);

        User updated = profileService.updateProfile(student, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(3L);
        assertThat(saved.getAvatarUrl()).isEqualTo("/uploads/avatar.jpg");
        assertThat(saved.getPhone()).isEqualTo("13800001111");
        assertThat(saved.getBio()).isEqualTo("热爱校园公益活动");
        assertThat(saved.getStudentNo()).isNull();
        assertThat(saved.getRealName()).isNull();
        assertThat(updated.getPassword()).isNull();
    }

    @Test
    void changesPasswordWithBcryptAfterOldPasswordValidated() {
        User student = student();
        student.setPassword(passwordService.encode("123456"));
        StudentPasswordRequest request = new StudentPasswordRequest();
        request.setOldPassword("123456");
        request.setNewPassword("abcdef");
        request.setConfirmPassword("abcdef");

        profileService.changePassword(student, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isNotEqualTo("abcdef");
        assertThat(passwordService.matches("abcdef", saved.getPassword())).isTrue();
    }

    @Test
    void rejectsPasswordChangeWhenOldPasswordIsWrong() {
        User student = student();
        student.setPassword(passwordService.encode("123456"));
        StudentPasswordRequest request = new StudentPasswordRequest();
        request.setOldPassword("wrong");
        request.setNewPassword("abcdef");
        request.setConfirmPassword("abcdef");

        assertThatThrownBy(() -> profileService.changePassword(student, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("原密码不正确");
    }

    @Test
    void summarizesStudentActivityData() {
        User student = student();
        StudentActivityReward reward = new StudentActivityReward();
        reward.setRewardHours(new BigDecimal("2.5"));
        reward.setRewardPoints(10);
        when(registrationMapper.selectCount(any())).thenReturn(3L);
        when(checkinMapper.selectCount(any())).thenReturn(2L);
        when(noticeReceiverMapper.selectCount(any())).thenReturn(4L);
        when(rewardMapper.selectList(any())).thenReturn(List.of(reward));

        StudentProfileSummary summary = profileService.summary(student);

        assertThat(summary.getRegistrationCount()).isEqualTo(3L);
        assertThat(summary.getCheckinCount()).isEqualTo(2L);
        assertThat(summary.getRewardHours()).isEqualByComparingTo("2.5");
        assertThat(summary.getRewardPoints()).isEqualTo(10);
        assertThat(summary.getUnreadMessageCount()).isEqualTo(4L);
    }

    @Test
    void uploadsAvatarAndUpdatesOnlyCurrentStudentAvatar() {
        User student = student();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1});
        FileUploadResult result = new FileUploadResult();
        result.setUrl("/uploads/avatar/avatar.png");
        when(fileService.uploadAvatar(file)).thenReturn(result);

        FileUploadResult uploaded = profileService.uploadAvatar(student, file);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(uploaded.getUrl()).isEqualTo("/uploads/avatar/avatar.png");
        assertThat(captor.getValue().getId()).isEqualTo(3L);
        assertThat(captor.getValue().getAvatarUrl()).isEqualTo("/uploads/avatar/avatar.png");
        assertThat(captor.getValue().getStudentNo()).isNull();
        assertThat(captor.getValue().getRealName()).isNull();
    }

    private User student() {
        User student = new User();
        student.setId(3L);
        student.setUsername("2321241389");
        student.setStudentNo("2321241389");
        student.setRealName("张三");
        student.setRole("student");
        student.setCampus("龙子湖校区");
        student.setCollege("信息与管理科学学院");
        student.setMajorName("软件工程");
        return student;
    }
}
