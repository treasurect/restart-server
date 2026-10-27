package com.treasure.restart.func.ai;

import com.treasure.restart.base.BaseResponse;
import com.treasure.restart.dto.AiChatRequest;
import com.treasure.restart.dto.AiChatResponse;
import com.treasure.restart.entity.AiConversation;
import com.treasure.restart.entity.AiMessage;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.apache.ibatis.annotations.Param;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIChatController {
    @Resource
    private AiChatService aiChatService;

    @PostMapping("/chat/guest")
    public BaseResponse<AiChatResponse> chatAsGuest(@Valid @RequestBody AiChatRequest aiChatRequest) {
        AiChatResponse response = aiChatService.chatAsGuest(aiChatRequest);
        return BaseResponse.success("获取成功", response);
    }

    @PostMapping("/chat/user")
    public BaseResponse<AiChatResponse> chatAsUser(@Valid @RequestBody AiChatRequest aiChatRequest) {
        AiChatResponse response = aiChatService.chatAsUser(aiChatRequest);
        return BaseResponse.success("获取成功", response);
    }

    @PostMapping(value = "/chat/user/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);
        aiChatService.chatStream(request, emitter);
        return emitter;
    }

    @GetMapping("/conversation/list")
    public BaseResponse<List<AiConversation>> loadCurrentConversations() {
        return BaseResponse.success("获取成功", aiChatService.loadConversations());
    }

    @GetMapping("/message/list")
    public BaseResponse<List<AiMessage>> loadMessagesByCid(@RequestParam Long conversationId) {
        return BaseResponse.success("获取成功", aiChatService.loadMessagesByCid(conversationId));
    }

    @PutMapping("/conversation/title")
    public BaseResponse<Void> updateConversationTitle(@RequestParam Long conversationId, @RequestParam String title) {
        aiChatService.updateConversationTitle(conversationId, title);
        return BaseResponse.success("修改成功");
    }

    @DeleteMapping("/conversation")
    public BaseResponse<Void> deleteConversation(@RequestParam Long conversationId) {
        aiChatService.deleteConversation(conversationId);
        return BaseResponse.success("删除成功");
    }
}
