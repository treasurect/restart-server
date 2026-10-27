package com.treasure.restart.func.ai;

import com.treasure.restart.entity.AiMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiMessageMapper {

    @Insert("""
            INSERT INTO ai_message (conversation_id,role,content,status)
            VALUES (#{conversationId},#{role},#{content},#{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMessage(AiMessage aiMessage);

    @Select("""
            SELECT id,conversation_id AS conversationId,role,content,created_at AS createdAt
            FROM ai_message
            WHERE conversation_id=#{conversationId}
            ORDER BY id ASC
            """)
    List<AiMessage> loadMessagesByCid(@Param("conversationId") Long conversationId);


    @Select("""
            SELECT id,conversation_id AS conversationId,role,content,created_at AS createdAt
            FROM ai_message
            WHERE conversation_id=#{conversationId}
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<AiMessage> loadLatestMessages(@Param("conversationId") Long conversationId, @Param("limit") int limit);

    @Select("""
            SELECT id,conversation_id AS conversationId,role,content,created_at AS createdAt
            FROM ai_message
            WHERE conversation_id=#{conversationId} AND status='COMPLETED'
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<AiMessage> loadLatestCompletedMessages(@Param("conversationId") Long conversationId, @Param("limit") int limit);

    @Delete("""
            DELETE FROM ai_message
            WHERE conversation_id = #{conversationId}
            """)
    int deleteByCid(@Param("conversationId") Long conversationId);
}
