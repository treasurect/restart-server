package com.treasure.restart.dto;

import org.jspecify.annotations.Nullable;

public record AiChatResponse(
        @Nullable Long conversationId,
        String reply
) {
}
