package com.treasure.restart.func.album;

import com.treasure.restart.dto.AlbumListResponse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlbumServiceImpl implements AlbumService {
    @Resource
    private AlbumMapper albumMapper;

    @Override
    public AlbumListResponse getAlbumList() {
        AlbumListResponse response = new AlbumListResponse();
        response.setImageList(albumMapper.getAlbumList());
        response.setTotal(albumMapper.getCount());
        return response;
    }
}
