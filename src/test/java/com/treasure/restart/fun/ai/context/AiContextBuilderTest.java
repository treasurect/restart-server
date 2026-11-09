//
//package com.treasure.restart.fun.ai.context;
//
//import com.treasure.restart.entity.AiMessage;
//import com.treasure.restart.func.ai.context.AiContextBuilder;
//import com.treasure.restart.func.ai.context.AiTokenCounterProd;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class AiContextBuilderTest {
//
//    private AiTokenCounterProd tokenCounter;
//    private AiContextBuilder contextBuilder;
//    private final Map<String, Integer> tokenCosts = new HashMap<>();
//
//    @BeforeEach
//    void setUp() {
//        tokenCosts.clear();
//
//        tokenCounter = mock(AiTokenCounterProd.class);
//
//        // 使用可控的 Token 数，避免测试依赖真实 tokenizer 的分词结果。
//        when(tokenCounter.count(anyString())).thenAnswer(invocation -> {
//            String text = invocation.getArgument(0);
//            return tokenCosts.getOrDefault(text, 1);
//        });
//
//        contextBuilder = new AiContextBuilder(tokenCounter);
//    }
//
//    /**
//     * 测试：历史消息按时间正序，当前问题位于最后。
//     */
//    @Test
//    void shouldKeepMessagesInCorrectOrder() {
//        tokenCosts.put("system", 1);
//        tokenCosts.put("current", 1);
//        tokenCosts.put("user-old", 2);
//        tokenCosts.put("assistant-old", 2);
//        tokenCosts.put("user-new", 2);
//        tokenCosts.put("assistant-new", 2);
//
//        List<AiMessage> history = List.of(
//                message("user", "user-old"),
//                message("assistant", "assistant-old"),
//                message("user", "user-new"),
//                message("assistant", "assistant-new")
//        );
//
//        // system + current = 2，历史最多使用 4。
//        List<Map<String, String>> result =
//                contextBuilder.build("system", history, "current", 6);
//
//        assertEquals("system", result.get(0).get("content"));
//        assertEquals("user-new", result.get(1).get("content"));
//        assertEquals("assistant-new", result.get(2).get("content"));
//        assertEquals("current", result.get(3).get("content"));
//
//        assertEquals("system", result.get(0).get("role"));
//        assertEquals("user", result.get(1).get("role"));
//        assertEquals("assistant", result.get(2).get("role"));
//        assertEquals("user", result.get(3).get("role"));
//    }
//
//    /**
//     * 测试：最终文本 Token 总数不得超过预算。
//     */
//    @Test
//    void shouldNotExceedTokenBudget() {
//        tokenCosts.put("system", 3);
//        tokenCosts.put("current", 2);
//        tokenCosts.put("user-1", 2);
//        tokenCosts.put("assistant-1", 2);
//        tokenCosts.put("user-2", 2);
//        tokenCosts.put("assistant-2", 2);
//
//        List<AiMessage> history = List.of(
//                message("user", "user-1"),
//                message("assistant", "assistant-1"),
//                message("user", "user-2"),
//                message("assistant", "assistant-2")
//        );
//
//        int maxTokens = 9;
//
//        List<Map<String, String>> result =
//                contextBuilder.build("system", history, "current", maxTokens);
//
//        int actualTokens = result.stream()
//                .mapToInt(item -> tokenCosts.getOrDefault(
//                        item.get("content"), 1))
//                .sum();
//
//        assertTrue(
//                actualTokens <= maxTokens,
//                "实际 Token 数 " + actualTokens
//                        + " 超过预算 " + maxTokens
//        );
//
//        assertTrue(result.stream().anyMatch(item ->
//                "system".equals(item.get("role"))
//                        && "system".equals(item.get("content"))));
//
//        assertEquals(
//                "current",
//                result.get(result.size() - 1).get("content")
//        );
//    }
//
//    /**
//     * 测试：即使历史预算不足，也必须保留 system 和当前问题。
//     */
//    @Test
//    void shouldAlwaysKeepSystemPromptAndCurrentMessage() {
//        tokenCosts.put("system", 3);
//        tokenCosts.put("current", 3);
//        tokenCosts.put("old-user", 5);
//        tokenCosts.put("old-assistant", 5);
//
//        List<AiMessage> history = List.of(
//                message("user", "old-user"),
//                message("assistant", "old-assistant")
//        );
//
//        List<Map<String, String>> result =
//                contextBuilder.build("system", history, "current", 6);
//
//        assertEquals(2, result.size());
//
//        assertEquals("system", result.get(0).get("role"));
//        assertEquals("system", result.get(0).get("content"));
//
//        assertEquals("user", result.get(1).get("role"));
//        assertEquals("current", result.get(1).get("content"));
//    }
//
//    /**
//     * 测试：system 和当前问题自身超预算时必须报错。
//     */
//    @Test
//    void shouldRejectWhenRequiredMessagesExceedBudget() {
//        tokenCosts.put("system", 4);
//        tokenCosts.put("current", 4);
//
//        IllegalArgumentException exception = assertThrows(
//                IllegalArgumentException.class,
//                () -> contextBuilder.build(
//                        "system",
//                        List.of(),
//                        "current",
//                        7
//                )
//        );
//
//        assertTrue(exception.getMessage().contains("超过输入 Token 预算"));
//    }
//
//    /**
//     * 测试：裁剪后不能以孤立的 assistant 消息开头。
//     */
//    @Test
//    void shouldNotStartHistoryWithAssistantMessage() {
//        tokenCosts.put("system", 1);
//        tokenCosts.put("current", 1);
//        tokenCosts.put("old-user", 5);
//        tokenCosts.put("latest-assistant", 2);
//
//        List<AiMessage> history = List.of(
//                message("user", "old-user"),
//                message("assistant", "latest-assistant")
//        );
//
//        // system + current 占 2，历史只剩 2 个 Token。
//        List<Map<String, String>> result =
//                contextBuilder.build("system", history, "current", 4);
//
//        assertEquals(2, result.size());
//        assertEquals("system", result.get(0).get("role"));
//        assertEquals("current", result.get(1).get("content"));
//    }
//
//    private AiMessage message(String role, String content) {
//        AiMessage message = new AiMessage();
//        message.setRole(role);
//        message.setContent(content);
//        return message;
//    }
//}
