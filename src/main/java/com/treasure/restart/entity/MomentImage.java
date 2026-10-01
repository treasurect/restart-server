package com.treasure.restart.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MomentImage {

    /**
     * 图片ID
     */
    private Long id;

    /**
     * 朋友圈ID
     */
    private Long momentId;

    /**
     * 图片地址
     */
    private String imageUrl;

    /**
     * 图片排序
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}