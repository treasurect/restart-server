package com.treasure.restart.func.more;

import com.treasure.restart.dto.AlbumItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExtMapper {

    @Select("""
            SELECT
                mi.id AS id,
                mi.moment_id AS momentId,
                mi.image_url AS imageUrl,
                mi.sort,
                mi.created_at AS createdAt,
                m.content AS momentContent,
                u.id AS createUserId,
                u.username AS createUserName,
                u.avatar AS createUserAvatar
                FROM moment_images mi
                INNER JOIN moments m ON mi.moment_id = m.id
                INNER JOIN sys_user u ON m.user_id = u.id
                INNER JOIN (SELECT id FROM moments ORDER BY RAND() LIMIT 3) rm ON m.id = rm.id
                            WHERE mi.sort = (SELECT MIN(mi2.sort) FROM moment_images mi2 WHERE mi2.moment_id = mi.moment_id)
                ORDER BY RAND()
            """)
    List<AlbumItem> getCarouselList();
}
