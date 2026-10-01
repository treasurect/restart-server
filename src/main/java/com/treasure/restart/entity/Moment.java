package com.treasure.restart.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Moment {

    /**
     * 朋友圈ID
     */
    private Long id;

    /**
     * 发布用户ID
     */
    private Long userId;

    /**
     * 朋友圈文字
     */
    private String content;

    /**
     * 发布时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    private String location;
}