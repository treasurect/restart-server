package com.treasure.restart.func.moment;

import com.treasure.restart.entity.Moment;
import com.treasure.restart.entity.MomentListRow;
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
        m.id,
        m.content,
        m.created_at AS createdAt,
        m.user_id AS userId,
        u.username AS userName,
        u.avatar AS userAvatar
    FROM moments m
    LEFT JOIN sys_user u ON u.id = m.user_id
    ORDER BY m.created_at DESC, m.id DESC
    LIMIT #{offset}, #{size}
    """)
    List<MomentListRow> getList(@Param("offset") Long offset, @Param("size") Integer size);

    /**
     * 获取朋友圈总数
     */
    @Select("SELECT COUNT(*) FROM moments")
    long getCount();
}
