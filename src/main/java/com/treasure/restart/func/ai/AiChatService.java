package com.treasure.restart.func.ai;

import com.treasure.restart.dto.AiChatRequest;
import com.treasure.restart.dto.AiChatResponse;
import com.treasure.restart.entity.AiConversation;
import com.treasure.restart.entity.AiMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AiChatService {
    AiChatResponse chatAsGuest(AiChatRequest request);

    AiChatResponse chatAsUser(AiChatRequest request);

    List<AiConversation> loadConversations();

    List<AiMessage> loadMessagesByCid(Long conversationId);

    void updateConversationTitle(Long conversationId,String title);
    void deleteConversation(Long conversationId);

    void chatStream(AiChatRequest request, SseEmitter emitter);
}
