package com.treasure.restart.func.ai;

import com.treasure.restart.entity.AiConversation;
import com.treasure.restart.entity.AiMessage;
import com.treasure.restart.func.ai.context.AiTokenCounterProd;
import com.treasure.restart.func.ai.mapper.AiConversationMapper;
import com.treasure.restart.func.ai.mapper.AiMessageMapper;
import com.treasure.restart.helper.LogHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiSummaryService {

    private static final int KEEP_RECENT_TURNS = 2;
    private static final int SUMMARY_MAX_TOKENS = 1500;

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiTokenCounterProd tokenCounter;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AiSummaryService(
            AiConversationMapper conversationMapper,
            AiMessageMapper messageMapper,
            AiTokenCounterProd tokenCounter,
            RestClient.Builder restClientBuilder,
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.model:deepseek-flash}") String model
    ) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.tokenCounter = tokenCounter;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public boolean toCheckOrUpdateSummary(Long cid, Long userId) {
        AiConversation conversation = conversationMapper.getCurrentConversation(cid, userId);
        if (conversation == null) throw new IllegalArgumentException("会话不存在或无权访问");
        Long oldUptoMsgId = conversation.getSummaryUptoMessageId();
        if (oldUptoMsgId == null) oldUptoMsgId = 0L;

        List<List<AiMessage>> completeTurns = buildCompleteTurns(messageMapper.loadCompletedMessagesAfterId(cid, oldUptoMsgId));
        //预留最近消息2条 阈值2个
        int needSaveCount = completeTurns.size() - 2;
        if (needSaveCount < KEEP_RECENT_TURNS) return false;
        ArrayList<List<AiMessage>> needSaveTurns = new ArrayList<>(completeTurns.subList(0, needSaveCount));

        ArrayList<AiMessage> needSaveMessages = new ArrayList<>();
        for (List<AiMessage> needSaveTurn : needSaveTurns) {
            needSaveMessages.addAll(needSaveTurn);
        }
        if (needSaveMessages.isEmpty()) return false;
        Long newUptoMsgId = needSaveMessages.get(needSaveMessages.size() - 1).getId();
        if (newUptoMsgId == null || newUptoMsgId < oldUptoMsgId) return false;

        String oldSummary = conversation.getSummary();
        String newSummary = generateSummary(oldSummary, needSaveMessages);
        if (newSummary == null || newSummary.isBlank()) return false;
        int summaryTokens = tokenCounter.count(newSummary);
        int i = conversationMapper.updateSummary(cid, userId, newSummary, newUptoMsgId, summaryTokens, oldUptoMsgId);
        LogHelper.info("AiSummaryService -> 摘要更新" + (i == 1 ? "成功" : "失败") + "，conversationId={" + cid + "}, uptoMessageId={" + newUptoMsgId + "}, summaryTokens={" + summaryTokens + "}");
        return i == 1;
    }

    /**
     * 将完整的轮次[一条user+一条assistant] 存入到List
     * <p>
     * 输入必须按消息 ID 正序。
     * 一个轮次从 user 开始，至少包含一条 assistant 才算完整。
     */
    private List<List<AiMessage>> buildCompleteTurns(List<AiMessage> messages) {
        List<List<AiMessage>> allTurns = new ArrayList<>();

        if (messages == null || messages.isEmpty()) return allTurns;

        List<AiMessage> currentTurn = null;
        boolean hasAssistant = false;
        for (AiMessage message : messages) {
            if (message == null || message.getRole() == null || message.getContent() == null) continue;

            if ("user".equalsIgnoreCase(message.getRole())) {
                if (currentTurn != null && hasAssistant) allTurns.add(currentTurn);

                currentTurn = new ArrayList<>();
                currentTurn.add(message);
                hasAssistant = false;
                continue;
            }

            if (currentTurn == null) {
                continue;
            }

            if ("assistant".equalsIgnoreCase(message.getRole())) {
                currentTurn.add(message);
                hasAssistant = true;
            }
        }

        if (currentTurn != null && hasAssistant) allTurns.add(currentTurn);
        return allTurns;
    }

    private String generateSummary(String oldSummary, ArrayList<AiMessage> needSaveMessages) {
        StringBuilder builder = new StringBuilder();
        needSaveMessages.forEach(needSaveMessage -> {
            builder.append(needSaveMessage.getRole())
                    .append(": ")
                    .append(needSaveMessage.getContent())
                    .append("\n");
        });
        String systemPrompt = """
                你是一个对话历史摘要器。
                请将已有摘要与新增对话合并为一份准确、简洁、可供后续 AI 对话使用的摘要。
                
                优先保留：
                1. 用户明确提供的事实与长期目标；
                2. 已确认的技术选型、约束和设计决策；
                3. 已完成事项、当前问题和待办事项；
                4. 对后续问题有帮助的关键参数。
                
                不要把猜测写成事实，不要编造内容。
                删除重复信息，纠正已经明确被后续对话推翻的旧信息。
                输出纯文本摘要，不要添加解释。
                摘要应尽可能简洁，目标不超过 1200 Token。
                """;
        String userPrompt = """
                [已有摘要]:%s。
                [新增的完整对话轮次]:%s。
                请将这两部分合并为一份最新摘要
                """.formatted(oldSummary == null || oldSummary.isBlank() ? "无内容" : oldSummary, builder);
        HashMap<String, Object> map = new HashMap<>();
        map.put("model", model);
        map.put("stream", false);
        map.put("temperature", 0.1);
        map.put("max_tokens", SUMMARY_MAX_TOKENS);
        map.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(map)
                .retrieve()
                .body(JsonNode.class);
        if (response == null) throw new IllegalStateException("DeepSeek 摘要接口返回为空");
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (!content.isString()) throw new IllegalStateException("DeepSeek 摘要响应缺少 message.content");
        String summary = content.asString().trim();
        if (summary.isBlank()) throw new IllegalStateException("DeepSeek 返回空摘要");
        return summary;
    }
}
