package com.treasure.restart.func.moment;

import com.treasure.restart.dto.MomentListResponse;
import com.treasure.restart.dto.MomentPublishRequest;
import com.treasure.restart.entity.Moment;
import com.treasure.restart.entity.MomentImage;
import com.treasure.restart.helper.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MomentServiceImpl implements MomentService {
    private final MomentMapper momentMapper;
    private final MomentImageMapper momentImageMapper;

    public MomentServiceImpl(MomentMapper momentMapper, MomentImageMapper momentImageMapper) {
        this.momentMapper = momentMapper;
        this.momentImageMapper = momentImageMapper;
    }

    @Override
    @Transactional
    public Long publish(Long userId, MomentPublishRequest request) {
        if ((request.getContent() == null || request.getContent().isEmpty()) && (request.getImages() == null || request.getImages().isEmpty())) {
            throw new BusinessException(-1, "请输入有效内容");
        }
        Moment moment = new Moment();
        moment.setUserId(userId);
        moment.setContent(request.getContent());
        moment.setLocation(request.getLocation());
        momentMapper.insert(moment);

        Long momentId = moment.getId();
        List<String> images = request.getImages();
        if (images != null && !images.isEmpty()) {
            if (images.size() > 9) throw new BusinessException(-1, "最多上传9张图片");
            for (int i = 0; i < images.size(); i++) {
                MomentImage momentImage = new MomentImage();
                momentImage.setMomentId(momentId);
                momentImage.setImageUrl(images.get(i));
                momentImage.setSort(i);
                momentImageMapper.insert(momentImage);
            }
        }

        return momentId;
    }

    @Override
    public MomentListResponse getMomentList(Integer page, Integer size) {
        long count = momentMapper.getCount();
        MomentListResponse momentListResponse = new MomentListResponse();
        momentListResponse.setPage(page);
        momentListResponse.setTotal(count);

        int offset = (page - 1) * size;
        List<Moment> momentList = momentMapper.getList(offset, size);
        if (momentList == null || momentList.isEmpty()) {
            momentListResponse.setMomentList(Collections.emptyList());
            return momentListResponse;
        }
        List<Long> momentIds = momentList.stream().map(Moment::getId).toList();
        List<MomentImage> imageList = momentImageMapper.selectByMomentIds(momentIds);
        Map<Long, List<String>> imageMap = imageList.stream().collect(
                Collectors.groupingBy(
                        MomentImage::getMomentId,
                        Collectors.mapping(MomentImage::getImageUrl, Collectors.toList())
                )
        );
        List<MomentListResponse.MomentItemResponse> items = new ArrayList<>();
        for (Moment moment : momentList) {
            MomentListResponse.MomentItemResponse momentItemResponse = new MomentListResponse.MomentItemResponse();
            momentItemResponse.setContent(moment.getContent());
            momentItemResponse.setCreatedAt(moment.getCreatedAt());
            momentItemResponse.setId(moment.getId());
            momentItemResponse.setImages(imageMap.getOrDefault(moment.getId(), Collections.emptyList()));
            items.add(momentItemResponse);
        }
        momentListResponse.setMomentList(items);
        return momentListResponse;
    }
}
