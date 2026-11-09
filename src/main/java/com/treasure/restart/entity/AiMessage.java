package com.treasure.restart.entity;

import com.treasure.restart.enums.AiMessageStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class AiMessage {
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
    private AiMessageStatus status;
    private String messageType;
    private String extra;
}
