package com.treasure.restart.dto;

import com.treasure.restart.entity.AlbumItem;
import lombok.Data;

import java.util.List;

@Data
public class AlbumListResponse {

    private Long total;

    private List<AlbumItem> imageList;
}
