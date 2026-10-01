package com.treasure.restart.func.moment;

import com.treasure.restart.dto.MomentListResponse;
import com.treasure.restart.dto.MomentPublishRequest;
import com.treasure.restart.entity.Moment;

public interface MomentService {

    /**
     * 发布朋友圈
     *
     * @param userId 当前登录用户ID
     * @param request 发布请求
     * @return 朋友圈ID
     */
    Long publish(Long userId, MomentPublishRequest request);


    /**
     * 查询朋友圈列表
     */
     MomentListResponse getMomentList(Integer page, Integer size);
}
