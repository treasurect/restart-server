
package com.treasure.restart.func.ai.context;

import com.treasure.restart.entity.AiMessage;
import com.treasure.restart.helper.LogHelper;
import com.treasure.restart.helper.exception.BusinessException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AiContextBuilder {

    private final AiTokenCounterProd tokenCounter;
    private final ObjectMapper objectMapper;

    public AiContextBuilder(AiTokenCounterProd tokenCounter, ObjectMapper objectMapper) {
        this.tokenCounter = tokenCounter;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> build(
            String systemPrompt,
            List<AiMessage> history,
            String currentMessage,
            int maxInputTokens
    ) {
        return build(systemPrompt, null, history, currentMessage, null, maxInputTokens);
    }

    public List<Map<String, Object>> build(
            String systemPrompt,
            String summary,
            List<AiMessage> history,
            String currentMessage,
            int maxInputTokens
    ) {
        return build(systemPrompt, summary, history, currentMessage, null, maxInputTokens);
    }

    /**
     * 构建发送给 LLM 的上下文。
     * <p>
     * maxInputTokens 包含 system、摘要、历史消息、
     * 当前用户消息的文本 Token。
     */
    public List<Map<String, Object>> build(
            String systemPrompt,
            String summary,
            List<AiMessage> history,
            String currentMessage,
            List<String> currentImageUrls,
            int maxInputTokens
    ) {
        if (maxInputTokens <= 0) throw new IllegalArgumentException("maxInputTokens 必须大于 0");

        String safeSystemPrompt = systemPrompt == null ? "" : systemPrompt;
        String safeCurrentMessage = currentMessage == null ? "" : currentMessage;
        int systemTokens = tokenCounter.count(safeSystemPrompt);
        int currentTokens = tokenCounter.count(safeCurrentMessage);

        if ((systemTokens + currentTokens) > maxInputTokens) throw new IllegalArgumentException(
                "系统提示词和当前用户消息已超过输入 Token 预算，"
                        + "systemTokens=" + systemTokens
                        + ", currentTokens=" + currentTokens
                        + ", maxInputTokens=" + maxInputTokens
        );

        int summaryTokens = 0;
        String safeSummary = null;
        if (summary != null && !summary.isBlank()) {
            int countedSummaryTokens = tokenCounter.count(summary);
            if (systemTokens + currentTokens + countedSummaryTokens <= maxInputTokens) {
                safeSummary = summary;
                summaryTokens = countedSummaryTokens;
            }
        }

        int remainingTokens = maxInputTokens - systemTokens - currentTokens - summaryTokens;
        List<List<AiMessage>> turns = buildCompleteTurns(history);
        List<List<AiMessage>> selectedTurns = new ArrayList<>();

        int historyTokens = 0;

        // 从最新轮次向前选，但最终按时间正序发送。
        for (int i = turns.size() - 1; i >= 0; i--) {
            List<AiMessage> turn = turns.get(i);

            int turnTokens = 0;
            for (AiMessage message : turn) {
                turnTokens += tokenCounter.count(message.getContent());
            }

            if (historyTokens + turnTokens > remainingTokens) {
                break;
            }

            selectedTurns.add(turn);
            historyTokens += turnTokens;
        }

        Collections.reverse(selectedTurns);
        //===================================================================================
        //组合deepseek 的消息 前序+概要+历史+当前
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", safeSystemPrompt));
        if (safeSummary != null) messages.add(Map.of(
                "role", "system",
                "content", """
                        以下是此前对话的历史摘要，仅用于理解背景。
                        如果摘要与当前用户明确提供的新信息冲突，
                        应优先采用当前用户提供的信息。
                        
                        历史摘要：
                        """ + safeSummary
        ));
        // 历史消息
        for (List<AiMessage> turn : selectedTurns) {
            for (AiMessage message : turn) {
                String role = message.getRole().toLowerCase();
                Object content;
                if ("user".equals(role)) {
                    List<String> imageUrls = getImageUrls(message.getExtra());
                    content = buildImageContent(message.getContent(), imageUrls);
                } else {
                    // assistant 消息：始终使用普通文本
                    content = message.getContent();
                }
                messages.add(Map.of("role", role, "content", content));
            }
        }
        messages.add(Map.of("role", "user", "content", buildImageContent(safeCurrentMessage, currentImageUrls)));
        LogHelper.info(
                "AiContextBuilder -> systemTokens={" + systemTokens
                        + "}, summaryTokens={" + summaryTokens
                        + "}, historyTokens={" + historyTokens
                        + "}, currentTokens={" + currentTokens
                        + "}, totalTextTokens={" + systemTokens + summaryTokens + historyTokens + currentTokens
                        + "}, maxInputTokens={" + maxInputTokens
                        + "}, selectedTurns={" + selectedTurns.size()
                        + "}, messageCount={" + messages.size() + "}"
        );

        return messages;
    }

    /**
     * 按 user 消息切分轮次。
     * 一轮必须以 user 开始，并且至少包含一条 assistant 消息。
     */
    private List<List<AiMessage>> buildCompleteTurns(List<AiMessage> history) {
        List<List<AiMessage>> turns = new ArrayList<>();

        if (history == null || history.isEmpty()) {
            return turns;
        }

        List<AiMessage> currentTurn = null;
        boolean hasAssistant = false;

        for (AiMessage message : history) {
            if (message == null || message.getRole() == null || message.getContent() == null) {
                continue;
            }

            String role = message.getRole();
            if ("user".equalsIgnoreCase(role)) {
                if (currentTurn != null && hasAssistant) turns.add(currentTurn);

                currentTurn = new ArrayList<>();
                currentTurn.add(message);
                hasAssistant = false;
                continue;
            }

            if (currentTurn == null) {
                continue;
            }

            if ("assistant".equalsIgnoreCase(role)) {
                currentTurn.add(message);
                hasAssistant = true;
            }
        }

        if (currentTurn != null && hasAssistant) turns.add(currentTurn);

        return turns;
    }

    public Object buildImageContent(String text, List<String> imageUrls) {
        String safeText = text == null ? "" : text;
        List<String> safeImageUrls = imageUrls == null
                ? List.of() : imageUrls.stream().filter(url -> url != null && !url.isBlank()).toList();

        if (safeImageUrls.isEmpty()) {
            if (safeText.isBlank()) {
                throw new BusinessException(400, "消息内容不能为空");
            }
            // 纯文本仍然传 String
            return safeText;
        }

        List<Map<String, Object>> content = new ArrayList<>();

        if (!safeText.isBlank()) {
            content.add(Map.of("type", "text", "text", safeText));
        }

        for (String imageUrl : safeImageUrls) {
            content.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of(
                            "url", imageUrl,
                            "detail", "auto"
                    )
            ));
        }

        return content;
    }

    private List<String> getImageUrls(String extra) {
        if (extra == null || extra.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(extra);
            JsonNode urls = root.get("imageUrls");
            if (urls == null || !urls.isArray()) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (JsonNode node : urls) {
                if (node.isString() && !node.asString().isBlank()) {
                    result.add(node.asString());
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException(500, "历史图片信息解析失败");
        }
    }
}
