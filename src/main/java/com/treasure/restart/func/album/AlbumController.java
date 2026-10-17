package com.treasure.restart.func.album;

import com.treasure.restart.base.BaseResponse;
import com.treasure.restart.dto.AlbumListResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/album")
public class AlbumController {
    @Resource
    private AlbumService albumService;

    @GetMapping("/list")
    public BaseResponse<AlbumListResponse>  getAlbumList() {
        AlbumListResponse response = albumService.getAlbumList();

        return BaseResponse.success("获取成功", response);

    }
}
