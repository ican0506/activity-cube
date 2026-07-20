package com.activitycube.util;

import com.activitycube.common.BusinessException;

import java.util.Map;

public final class StudentNoUtil {
    private static final Map<String, String> MAJOR_NAMES = Map.of(
            "21241", "软件工程",
            "21242", "计算机科学与技术",
            "21243", "数据科学与大数据技术"
    );

    private StudentNoUtil() {
    }

    public static ParsedStudentNo parse(String studentNo) {
        if (studentNo == null || !studentNo.matches("^\\d{10}$")) {
            throw new BusinessException("学号必须为10位数字");
        }
        String yearPrefix = studentNo.substring(0, 2);
        String majorCode = studentNo.substring(2, 7);
        String sequenceNo = studentNo.substring(7);
        String gradeYear = "20" + yearPrefix + "级";
        return new ParsedStudentNo(gradeYear, majorCode, MAJOR_NAMES.get(majorCode), sequenceNo);
    }

    public record ParsedStudentNo(String gradeYear, String majorCode, String majorName, String sequenceNo) {
    }
}
