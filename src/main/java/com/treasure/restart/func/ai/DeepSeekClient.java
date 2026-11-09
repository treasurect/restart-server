
package com.treasure.restart.func.ai;

import com.treasure.restart.func.ai.context.AiStreamContextManager;
import com.treasure.restart.helper.LogHelper;
import com.treasure.restart.helper.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final WebClient webClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.base-url}") String baseUrl,
            @Value("${deepseek.model}") String model,
            ObjectMapper objectMapper
    ) {
        this.model = model;
        this.objectMapper = objectMapper;

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 非流式请求
     */
    public String chat(List<Map<String, Object>> messages) {
        Map<String, Object> request = Map.of(
                "model", model,
                "messages", messages,
                "thinking", Map.of("type", "enabled"),
                "reasoning_effort", "high",
                "stream", false
        );

        JsonNode response = webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) {
            throw new BusinessException("DeepSeek 返回了空响应");
        }

        JsonNode content = response.at("/choices/0/message/content");

        if (content.isMissingNode() || content.isNull()) {
            throw new BusinessException("DeepSeek 响应中没有回答内容");
        }

        return content.asString();
    }

    /**
     * DeepSeek 流式请求。
     * 返回已生成的完整文本；取消或超时时返回已接收的部分文本。
     */
    public String chatStream(List<Map<String, Object>> messages, Consumer<String> onToken, AiStreamContextManager context) {
        StringBuilder builder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Map<String, Object> request = Map.of(
                "model", model,
                "messages", messages,
                "thinking", Map.of("type", "enabled"),
                "reasoning_effort", "high",
                "stream", true
        );

        Disposable disposable = webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new BusinessException(
                                        "DeepSeek 请求失败："
                                                + response.statusCode()
                                                + "，" + body
                                ))
                )
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })

                // handle 允许跳过事件，不需要返回 null
                .<String>handle((event, sink) -> {
                    String data = event.data();

                    if (data == null || data.isBlank() || "[DONE]".equals(data.trim())) {
                        return;
                    }

                    try {
                        JsonNode root = objectMapper.readTree(data);
                        JsonNode contentNode = root.at("/choices/0/delta/content");

                        if (contentNode.isMissingNode() || contentNode.isNull()) {
                            return;
                        }

                        String content = contentNode.asString();

                        if (content != null && !content.isEmpty()) {
                            sink.next(content);
                        }
                    } catch (Exception e) {
                        sink.error(new BusinessException(
                                "解析 DeepSeek SSE 数据失败：" + e.getMessage()
                        ));
                    }
                })
                .subscribe(
                        token -> {
                            if (!context.isGenerating()) {
                                return;
                            }

                            synchronized (builder) {
                                builder.append(token);
                            }

                            try {
                                context.append(token);
                                onToken.accept(token);
                            } catch (Exception e) {
                                log.error("处理 AI token 失败", e);
                                errorRef.compareAndSet(null, e);
                                context.fail();
                            }
                        },
                        error -> {
                            log.error("DeepSeek 流式请求异常", error);

                            errorRef.compareAndSet(null, error);

                            if (context.isGenerating()) {
                                context.fail();
                            }

                            // 关键：异常结束时也要唤醒等待线程
                            latch.countDown();
                        },
                        latch::countDown
                );

        // 保存实际订阅，以便取消时终止上游请求
        context.setSubscription(disposable);

        try {
            boolean finished = latch.await(180, TimeUnit.SECONDS);

            if (!finished) {
                context.timeout();
                disposable.dispose();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            context.cancel();
            disposable.dispose();
            throw new BusinessException("DeepSeek 流式请求线程被中断");
        }

        // 取消或超时：返回已生成的部分内容
        if (context.isCancelled() || context.isTimeout()) {
            synchronized (builder) {
                return builder.toString();
            }
        }

        Throwable error = errorRef.get();

        if (error != null) {
            if (error instanceof BusinessException businessException) {
                throw businessException;
            }

            throw new BusinessException(
                    "读取 DeepSeek 流式响应失败：" + error.getMessage()
            );
        }

        if (context.isFailed()) {
            throw new BusinessException("DeepSeek 流式请求失败");
        }

        context.complete();

        if (!context.isComplete()) {
            if (context.isCancelled() || context.isTimeout()) {
                synchronized (builder) {
                    return builder.toString();
                }
            }

            throw new BusinessException("DeepSeek 流式请求未正常完成");
        }

        synchronized (builder) {
            return builder.toString();
        }
    }
}
