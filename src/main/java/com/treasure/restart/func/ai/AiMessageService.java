package com.treasure.restart.func.ai;

import com.treasure.restart.entity.AiMessage;
import com.treasure.restart.enums.AiMessageStatus;
import com.treasure.restart.func.ai.mapper.AiConversationMapper;
import com.treasure.restart.func.ai.mapper.AiMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiMessageService {

    private final AiMessageMapper aiMessageMapper;
    private final AiConversationMapper aiConversationMapper;
    private final ObjectMapper objectMapper;

    /**
     * 保存普通文本消息
     */
    @Transactional
    public AiMessage saveMessage(Long userId, String role, Long conversationId, String content, AiMessageStatus status) {
        return saveMessage(userId, role, conversationId, content, null, status);
    }

    /**
     * 保存文本 + 图片消息
     */
    @Transactional
    public AiMessage saveMessage(Long userId, String role, Long conversationId, String content, List<String> imageUrls, AiMessageStatus status) {
        AiMessage message = new AiMessage();

        message.setRole(role);
        message.setConversationId(conversationId);
        message.setContent(content == null ? "" : content);
        message.setStatus(status);

        if (imageUrls != null && !imageUrls.isEmpty()) {
            message.setMessageType("image");

            try {
                message.setExtra(objectMapper.writeValueAsString(Map.of("imageUrls", imageUrls)));
            } catch (Exception e) {
                throw new RuntimeException("图片信息序列化失败", e);
            }
        } else {
            message.setMessageType("text");
            message.setExtra(null);
        }

        aiMessageMapper.insertMessage(message);
        aiConversationMapper.updateUpdatedAt(conversationId, userId);

        return message;
    }
}
