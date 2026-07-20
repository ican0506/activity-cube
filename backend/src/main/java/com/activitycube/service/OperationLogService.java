package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.entity.OperationLog;
import com.activitycube.entity.User;
import com.activitycube.mapper.OperationLogMapper;
import com.activitycube.vo.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final OperationLogMapper operationLogMapper;

    public void record(User user, String operation, String targetType, Long targetId, String detail) {
        if (user == null) {
            return;
        }
        OperationLog log = new OperationLog();
        log.setUserId(user.getId());
        log.setUsername(user.getUsername());
        log.setRole(user.getRole());
        log.setOperation(operation);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIp(currentIp());
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    public PageResult<OperationLog> list(String operation, String username, LocalDateTime startTime,
                                          LocalDateTime endTime, long page, long size, User operator) {
        if (operator == null || !"admin".equals(operator.getRole())) {
            throw new BusinessException("只有管理员可以查看操作日志");
        }
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreatedAt);
        if (StringUtils.hasText(operation)) {
            wrapper.eq(OperationLog::getOperation, operation);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getCreatedAt, endTime);
        }
        long current = Math.max(page, 1);
        long pageSize = Math.min(Math.max(size, 1), 100);
        Page<OperationLog> result = operationLogMapper.selectPage(new Page<>(current, pageSize), wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), current, pageSize);
    }

    private String currentIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
