package com.treasure.restart.dto;

import com.treasure.restart.entity.Moment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MomentListResponse {
    private int page;
    private long total;
    private List<MomentItemResponse> momentList;

    @Data
    public static class MomentItemResponse {
        /**
         * 朋友圈ID
         */
        private Long id;

        /**
         * 发布用户ID
         */
        private Long userId;

        /**
         * 用户名
         */
        private String username;

        /**
         * 朋友圈文字
         */
        private String content;

        /**
         * 图片列表
         */
        private List<String> images;

        /**
         * 发布时间
         */
        private LocalDateTime createdAt;
    }
}
