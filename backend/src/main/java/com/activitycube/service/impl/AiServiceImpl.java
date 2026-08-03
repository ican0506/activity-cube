package com.activitycube.service.impl;

import com.activitycube.common.BusinessException;
import com.activitycube.config.AiProperties;
import com.activitycube.service.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public AiServiceImpl(AiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newHttpClient());
    }

    public AiServiceImpl(AiProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String completeJson(String prompt) {
        ensureConfigured();
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", properties.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", "你是高校校园活动运营文案助手，只输出严格 JSON。"),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.6
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chatCompletionsUrl()))
                    .timeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("AI 服务暂时不可用，请稍后重试。");
            }
            return extractContent(response.body());
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException("AI 服务暂时不可用，请稍后重试。");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("AI 服务暂时不可用，请稍后重试。");
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("AI 服务配置不正确，请检查 AI_BASE_URL。");
        }
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()) {
            throw new BusinessException("AI 功能未启用，请配置 AI_ENABLED=true 后再使用。");
        }
        if (!StringUtils.hasText(properties.getBaseUrl())
                || !StringUtils.hasText(properties.getApiKey())
                || !StringUtils.hasText(properties.getModel())) {
            throw new BusinessException("AI 功能未配置完整，请检查 AI_BASE_URL、AI_API_KEY 和 AI_MODEL。");
        }
    }

    private String chatCompletionsUrl() {
        String baseUrl = properties.getBaseUrl().trim();
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + "chat/completions";
        }
        return baseUrl + "/chat/completions";
    }

    private String extractContent(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isTextual() || !StringUtils.hasText(content.asText())) {
            throw new BusinessException("AI 返回格式异常，请稍后重试。");
        }
        return stripCodeFence(content.asText());
    }

    private String stripCodeFence(String content) {
        String value = content.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "");
            value = value.replaceFirst("\\s*```$", "");
        }
        return value.trim();
    }
}
