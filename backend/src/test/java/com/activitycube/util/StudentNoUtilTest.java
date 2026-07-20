package com.activitycube.util;

import com.activitycube.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentNoUtilTest {

    @Test
    void parsesSupportedTenDigitStudentNumber() {
        StudentNoUtil.ParsedStudentNo parsed = StudentNoUtil.parse("2321241389");

        assertThat(parsed.gradeYear()).isEqualTo("2023级");
        assertThat(parsed.majorCode()).isEqualTo("21241");
        assertThat(parsed.majorName()).isEqualTo("软件工程");
        assertThat(parsed.sequenceNo()).isEqualTo("389");
    }

    @Test
    void rejectsInvalidStudentNumberFormat() {
        assertThatThrownBy(() -> StudentNoUtil.parse("T2024001"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("学号必须为10位数字");
    }
}
