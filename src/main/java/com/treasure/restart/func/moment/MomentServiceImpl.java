package com.treasure.restart.func.moment;

import com.treasure.restart.dto.MomentListResponse;
import com.treasure.restart.dto.MomentPublishRequest;
import com.treasure.restart.entity.Moment;
import com.treasure.restart.entity.MomentImage;
import com.treasure.restart.entity.MomentListRow;
import com.treasure.restart.helper.UserContext;
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
    public MomentListResponse getMomentList(Integer page, Integer size, String type) {
        if (page == null || page < 1) {
            throw new BusinessException(400, "页码必须大于0");
        }

        if (size == null || size < 1 || size > 50) {
            throw new BusinessException(400, "每页条数必须在1到50之间");
        }
        long total = momentMapper.getCount();
        long totalPages = (total + size - 1) / size;
        MomentListResponse listResponse = new MomentListResponse();
        listResponse.setPage(page);
        listResponse.setSize(size);
        listResponse.setTotalPages(totalPages);
        listResponse.setTotal(total);
        listResponse.setHasMore(page < totalPages);

        long offset = (long) (page - 1) * size;
        List<MomentListRow> momentListRows = momentMapper.getList(offset, size, type, UserContext.getUserId());
        if (momentListRows == null || momentListRows.isEmpty()) {
            listResponse.setMomentList(Collections.emptyList());
            return listResponse;
        }
        List<Long> momentIds = momentListRows.stream().map(MomentListRow::getId).toList();
        List<MomentImage> imageList = momentImageMapper.selectByMomentIds(momentIds);
        Map<Long, List<String>> imageMap = imageList.stream().collect(
                Collectors.groupingBy(
                        MomentImage::getMomentId,
                        Collectors.mapping(MomentImage::getImageUrl, Collectors.toList())
                )
        );
        List<MomentListResponse.MomentItemResponse> items = new ArrayList<>();
        for (MomentListRow momentRow : momentListRows) {
            MomentListResponse.MomentItemResponse itemResponse = new MomentListResponse.MomentItemResponse();
            itemResponse.setId(momentRow.getId());
            itemResponse.setUserId(momentRow.getUserId());
            itemResponse.setUsername(momentRow.getUserName());
            itemResponse.setUserAvatar(momentRow.getUserAvatar());
            itemResponse.setContent(momentRow.getContent());
            itemResponse.setImages(imageMap.getOrDefault(momentRow.getId(), Collections.emptyList()));
            itemResponse.setCreatedAt(momentRow.getCreatedAt());
            items.add(itemResponse);
        }
        listResponse.setMomentList(items);
        return listResponse;
    }
}
