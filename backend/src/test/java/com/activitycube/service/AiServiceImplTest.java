package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.config.AiProperties;
import com.activitycube.service.impl.AiServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class AiServiceImplTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void disabledAiReturnsFriendlyMessageWithoutCallingModel() {
        AiProperties properties = configuredProperties();
        properties.setEnabled(false);
        AiService service = new AiServiceImpl(properties, objectMapper, mock(HttpClient.class));

        assertThatThrownBy(() -> service.completeJson("prompt"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 功能未启用，请配置 AI_ENABLED=true 后再使用。");
    }

    @Test
    void missingConfigurationReturnsFriendlyMessage() {
        AiProperties properties = configuredProperties();
        properties.setApiKey("");
        AiService service = new AiServiceImpl(properties, objectMapper, mock(HttpClient.class));

        assertThatThrownBy(() -> service.completeJson("prompt"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 功能未配置完整，请检查 AI_BASE_URL、AI_API_KEY 和 AI_MODEL。");
    }

    @Test
    void extractsContentFromOpenAiCompatibleResponse() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("""
                {"choices":[{"message":{"content":"{\\"title\\":\\"活动标题\\"}"}}]}
                """);
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        AiService service = new AiServiceImpl(configuredProperties(), objectMapper, httpClient);

        String result = service.completeJson("prompt");

        assertThat(result).isEqualTo("{\"title\":\"活动标题\"}");
    }

    @Test
    void failedModelCallReturnsFriendlyMessage() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new IOException("timeout"));
        AiService service = new AiServiceImpl(configuredProperties(), objectMapper, httpClient);

        assertThatThrownBy(() -> service.completeJson("prompt"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 服务暂时不可用，请稍后重试。");
    }

    @Test
    void malformedModelResponseReturnsFriendlyMessage() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"choices\":[]}");
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        AiService service = new AiServiceImpl(configuredProperties(), objectMapper, httpClient);

        assertThatThrownBy(() -> service.completeJson("prompt"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 返回格式异常，请稍后重试。");
    }

    private AiProperties configuredProperties() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://api.example.com/v1");
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        properties.setTimeoutSeconds(3);
        return properties;
    }
}
