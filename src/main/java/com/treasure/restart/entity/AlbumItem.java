package com.treasure.restart.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlbumItem {
    /**
     * 图片ID
     */
    private Long id;

    /**
     * 朋友圈ID
     */
    private Long momentId;

    //朋友圈内容
    private String momentContent;

    //发布者id
    private Long createUserId;
    private String createUserName;
    private String createUserAvatar;

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
