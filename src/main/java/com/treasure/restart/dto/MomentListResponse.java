package com.treasure.restart.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MomentListResponse {

    private Integer page;

    private Integer size;

    private Long total;

    private Long totalPages;

    private Boolean hasMore;

    private List<MomentItemResponse> momentList;

    @Data
    public static class MomentItemResponse {

        private Long id;

        private String content;

        private LocalDateTime createdAt;

        private List<String> images;

        private Long userId;

        private String username;

        private String userAvatar;
    }
}