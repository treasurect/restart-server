package com.treasure.restart.func.ai;

import com.treasure.restart.entity.AiConversation;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * ${} 是字符串直接拼接 SQL，而 #{} 才是 MyBatis 参数绑定。
 */
@Mapper
public interface AiConversationMapper {

    @Insert("""
            INSERT INTO ai_conversation (user_id,title) VALUES (#{userId},#{title})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertConversation(AiConversation aiConversation);

    @Select("""
            SELECT id,user_id AS userId,title,created_at AS createdAt,updated_at AS updatedAt
            FROM ai_conversation
            WHERE id = #{id} AND user_id = #{userId}
            LIMIT 1
            """)
    AiConversation getCurrentConversation(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
            SELECT id,user_id AS userId,title,created_at AS createdAt,updated_at AS updatedAt
            FROM ai_conversation
            WHERE user_id = #{userId}
            ORDER BY updated_at DESC
            """)
    List<AiConversation> loadConversionsByUid(@Param("userId") Long userId);

    @Update("""
            UPDATE ai_conversation
            SET title = #{title}
            WHERE id=#{id} AND user_id=#{userId}
            """)
    int updateTitle(@Param("id") Long id, @Param("userId") Long userId, @Param("title") String title);

    @Update("""
            UPDATE ai_conversation
            SET updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId}
            """)
    int updateUpdatedAt(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("""
            DELETE FROM ai_conversation
            WHERE id=#{id} AND user_id=#{userId}
            """)
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);
}
