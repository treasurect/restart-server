package com.treasure.restart.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MomentListRow {

    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private LocalDateTime createdAt;
}
