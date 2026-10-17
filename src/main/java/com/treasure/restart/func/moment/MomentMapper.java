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
            <script>
                SELECT
                    m.id,
                    m.content,
                    m.created_at AS createdAt,
                    m.user_id AS userId,
                    u.username AS userName,
                    u.avatar AS userAvatar
                FROM moments m
                LEFT JOIN sys_user u ON u.id = m.user_id
            <where>
                <if test="type == 'mine'">
                    AND m.user_id = #{userId}
                </if>
            
                <if test="type == 'related'">
                    AND (
                        m.user_id = #{userId}
                    )
                </if>
            
                <if test="type == 'latest'">
                    AND m.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                </if>
            </where>
            ORDER BY m.created_at DESC, m.id DESC
            LIMIT #{offset}, #{size}
            </script> 
            """)
    List<MomentListRow> getList(@Param("offset") Long offset, @Param("size") Integer size, @Param("type") String type, @Param("userId") Long userId);

    /**
     * 获取朋友圈总数
     */
    @Select("SELECT COUNT(*) FROM moments")
    long getCount();
}
