package com.treasure.restart.dto;

public record AiStreamEvent(String type, String content, Long conversationId, Long messageId) {
}
