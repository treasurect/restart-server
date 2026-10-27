package com.treasure.restart.func.ai;

import com.treasure.restart.helper.LogHelper;
import com.treasure.restart.helper.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Android
 * │
 * │ Retrofit SSE
 * ↓
 * Spring SseEmitter
 * │
 * │ executor
 * ↓
 * AI Service
 * │
 * ↓
 * DeepSeekClient
 * │
 * │ WebClient
 * ↓
 * DeepSeek
 * <p>
 * DeepSeek token
 * ↓
 * WebClient
 * ↓
 * DeepSeekClient
 * ↓
 * context.append(token)
 * ↓
 * emitter.send(token)
 * ↓
 * Android
 * <p>
 * <p>
 * Android Job.cancel()
 * ↓
 * SSE 连接断开
 * ↓
 * emitter.onCompletion()
 * ↓
 * context.cancel()
 * ↓
 * Disposable.dispose()
 * ↓
 * WebClient 取消订阅
 * ↓
 * 停止读取 DeepSeek 流
 * ↓
 * chatStream() 返回已经生成的内容
 * ↓
 * 保存 partialReply
 */
@Component
public class DeepSeekClient {
    //适用场景	RestClient -> 普通 REST API、CRUD、同步调用
    //          WebClient  -> SSE、响应式系统、流式数据
    private final WebClient webClient;
    //    private final RestClient restClient;
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
//        this.restClient = RestClient.builder()
//                .baseUrl(baseUrl)
//                .defaultHeader("Authorization", "Bearer " + apiKey)
//                .defaultHeader("Content-Type", "application/json")
//                .build();
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 全部内容一起返回
     *
     * @param messages
     * @return
     */
    public String chat(List<Map<String, String>> messages) {
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

    //    public String chatStream(List<Map<String, String>> messages, Consumer<String> onToken, BooleanSupplier isCancelled) {
//        StringBuilder builder = new StringBuilder();
//        restClient.post()
//                .uri("/chat/completions")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.TEXT_EVENT_STREAM)
//                .body(Map.of(
//                        "model", model,
//                        "messages", messages,
//                        "thinking", Map.of("type", "enabled"),
//                        "reasoning_effort", "high",
//                        "stream", true))
//                .exchange(((clientRequest, clientResponse) -> {
//                    if (!clientResponse.getStatusCode().is2xxSuccessful())
//                        throw new BusinessException("DeepSeek 请求失败：" + clientResponse.getStatusCode());
//                    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientResponse.getBody(), StandardCharsets.UTF_8))) {
//                        String line;
//                        while ((line = bufferedReader.readLine()) != null) {
//                            if (isCancelled.getAsBoolean()) {
//                                System.out.println("DeepSeek 流式请求已取消");
//                                break;
//                            }
//                            if (!line.startsWith("data:")) continue;
//                            String data = line.substring(5).trim();
//                            if ("[DONE]".equals(data)) break;
//                            if (data.isEmpty()) continue;
//                            JsonNode root = objectMapper.readTree(data);
//                            JsonNode contentNode = root.at("/choices/0/delta/content");
//                            if (contentNode.isMissingNode() || contentNode.isNull()) continue;
//                            String content = contentNode.asString();
//                            if (content.isEmpty()) continue;
//                            if (isCancelled.getAsBoolean()) break;
//                            builder.append(content);
//                            onToken.accept(content);
//                        }
//                    } catch (Exception e) {
//                        if (isCancelled.getAsBoolean()) return null;
//                        throw new BusinessException("读取 DeepSeek 流式响应失败" + e);
//                    }
//                    return null;
//                }));
//
//        return builder.toString();
//    }

    /**
     * DeepSeek 流式请求
     * <p>
     * context.cancel()
     * ↓
     * Disposable.dispose()
     * ↓
     * WebClient 取消上游 HTTP 流
     */
    public String chatStream(List<Map<String, String>> messages, Consumer<String> onToken, AiStreamContext context) {
        //因为 onToken 只负责逐段通知上层，而 chatStream() 最后还需要返回一个完整的 String，供 Service 保存数据库。
        //另外，用户中断时，也需要返回已经收到的部分内容。
        StringBuilder builder = new StringBuilder();

        //Java 的并发同步工具。
        //可以把它理解为一个等待信号：  初始计数为 1：尚未结束。 - 调用 countDown()：计数减为 0。- 等待中的线程发现计数为 0：可以继续执行。
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
                .onStatus(HttpStatusCode::isError,
                        response ->
                                response.bodyToMono(String.class)
                                        .defaultIfEmpty("")
                                        .map(body -> new BusinessException("DeepSeek 请求失败：" + response.statusCode() + "，" + body))
                )
                //ServerSentEvent<String>：一个 SSE 事件，事件数据是字符串。
                //Flux<T>：能够连续产生多个 T 的响应式数据流。
                //bodyToFlux(...)：将 HTTP 响应体转换成这样的数据流。
                .bodyToFlux(new org.springframework.core.ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                //doFinally() 会在响应式流终止或被取消等终止信号到达时执行。
//                .doFinally(signal -> {
//                    latch.countDown();
//                })
                .map(event -> {
                    String data = event.data();
                    if (data == null || data.isBlank()) return null;
                    if ("[DONE]".equals(data.trim())) return null;

                    try {
                        JsonNode root = objectMapper.readTree(data);
                        JsonNode contentNode = root.at("/choices/0/delta/content");
                        if (contentNode.isMissingNode() || contentNode.isNull()) return null;
                        String content = contentNode.asString();
                        return content == null || content.isEmpty() ? null : content;
                    } catch (Exception e) {
                        LogHelper.error("解析 DeepSeek SSE 数据失败" + e);
                        throw new BusinessException("解析 DeepSeek SSE 数据失败" + e);
                    }
                })
                .filter(token -> token != null && !token.isEmpty())
                .subscribe(
                        token -> {
                            if (!context.isGenerating()) return;
                            synchronized (builder) {
                                builder.append(token);
                            }
                            context.append(token);
                            onToken.accept(token);
                        },
                        error -> {
                            if (context.isGenerating()) {
                                errorRef.compareAndSet(null, error);
                                context.fail();
                            }
                        },
                        latch::countDown
                );
        // 保存真正的 WebClient Disposable
        context.setSubscription(disposable);

        try {
            boolean finished = latch.await(180, TimeUnit.SECONDS);
            if (!finished) context.timeout();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            context.cancel();
            throw new BusinessException("DeepSeek 流式请求线程被中断");
        }
        if (context.isCancelled() || context.isTimeout()) synchronized (builder) {
            return builder.toString();
        }

        Throwable error = errorRef.get();
        if (error != null) {
            if (error instanceof BusinessException businessException) throw businessException;
            throw new BusinessException("读取 DeepSeek 流式响应失败：" + error.getMessage());
        }

        if (context.isFailed()) throw new BusinessException("DeepSeek 流式请求失败");

        context.complete();

        if (!context.isComplete()) {
            if (context.isCancelled() || context.isTimeout()) synchronized (builder) {
                return builder.toString();
            }
            throw new BusinessException("DeepSeek 流式请求未正常完成");
        }

        synchronized (builder) {
            return builder.toString();
        }
    }
}