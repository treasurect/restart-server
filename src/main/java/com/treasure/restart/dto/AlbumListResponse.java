package com.treasure.restart.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AlbumListResponse {

    private Long total;

    private List<AlbumItem> imageList;
}
