package com.treasure.restart.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class AiConversation {
    private Long id;
    private Long userId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String summary;
    private Long summaryUptoMessageId;
    private int summaryTokens;
    private LocalDateTime summaryUpdatedAt;

}