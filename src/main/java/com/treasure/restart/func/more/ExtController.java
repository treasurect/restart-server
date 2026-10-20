package com.treasure.restart.func.more;

import com.treasure.restart.base.BaseResponse;
import com.treasure.restart.dto.AlbumItem;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ext")
public class ExtController {
    @Resource
    private ExtService extService;

    @GetMapping("carousel/list")
    BaseResponse<List<AlbumItem>> getCarouselList() {
        return BaseResponse.success("获取成功", extService.getCarouselList());
    }
}
