package com.treasure.restart.func.moment;

import com.treasure.restart.entity.Moment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MomentMapper {

    /**
     * 新增朋友圈
     */
    @Insert("""
            INSERT INTO `moments`
            (user_id, content, location)
            VALUES
            (#{userId}, #{content}, #{location})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Moment moment);


    /**
     * 分页查询朋友圈
     */
    @Select("""
            SELECT
                id,
                user_id,
                content,
                location,
                created_at,
                updated_at
            FROM moments
            ORDER BY created_at DESC
            LIMIT #{offset}, #{size}
            """)
    List<Moment> getList(@Param("offset") Integer offset, @Param("size") Integer size);

    /**
     * 获取朋友圈总数
     */
    @Select("""
        SELECT COUNT(*) FROM moments
        """)
    long getCount();
}
