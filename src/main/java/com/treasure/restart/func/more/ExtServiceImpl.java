package com.treasure.restart.func.more;

import com.treasure.restart.dto.AlbumItem;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExtServiceImpl implements ExtService {
    @Resource
    private ExtMapper extMapper;
    @Override
    public List<AlbumItem> getCarouselList() {
        return extMapper.getCarouselList();
    }
}
