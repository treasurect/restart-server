package com.treasure.restart.func.moment;

import com.treasure.restart.entity.MomentImage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MomentImageMapper {

    /**
     * 新增朋友圈图片
     */
    @Insert("""
        INSERT INTO `moment_images`
        (moment_id, image_url, sort)
        VALUES
        (#{momentId}, #{imageUrl}, #{sort})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MomentImage momentImage);


    /**
     * 根据朋友圈ID列表查询图片
     */
    @Select("""
        <script>
        SELECT
            id,
            moment_id,
            image_url,
            sort,
            created_at
        FROM moment_images
        WHERE moment_id IN
        <foreach collection="momentIds"
                 item="momentId"
                 open="("
                 separator=","
                 close=")">
            #{momentId}
        </foreach>
        ORDER BY moment_id ASC, sort ASC
        </script>
        """)
    List<MomentImage> selectByMomentIds(
            @Param("momentIds") List<Long> momentIds
    );
}
