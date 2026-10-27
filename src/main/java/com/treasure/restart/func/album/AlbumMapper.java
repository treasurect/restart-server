package com.treasure.restart.func.album;

import com.treasure.restart.entity.AlbumItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlbumMapper {
    @Select("""
    SELECT
        mi.id AS id,
        mi.moment_id AS momentId,
        mi.image_url AS imageUrl,
        mi.sort AS sort,
        m.created_at AS createAt,
        m.content AS momentContent,
        u.id AS createUserId,
        u.username AS createUserName,
        u.avatar AS createUserAvatar
        FROM moment_images mi
    INNER JOIN moments m ON mi.moment_id = m.id
    INNER JOIN sys_user u ON m.user_id = u.id
    ORDER BY m.created_at DESC , mi.sort ASC
    """)
    List<AlbumItem> getAlbumList();

    @Select("SELECT count(*) FROM moment_images")
    Long getCount();

}
