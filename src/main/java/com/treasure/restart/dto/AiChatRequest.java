package com.treasure.restart.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AiChatRequest(
        Long conversationId,
        @NotBlank(message = "消息不能为空")
        String message,
        List<HistoryMsgItem> history
) {
    public record HistoryMsgItem(
            String role,
            String content
    ) {
    }
}
