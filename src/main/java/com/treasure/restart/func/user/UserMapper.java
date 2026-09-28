package com.treasure.restart.func.user;

import com.treasure.restart.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("SELECT id, username, password, nickname, avatar, status, created_at, updated_at " +
            "FROM `sys_user` WHERE username = #{username} LIMIT 1")
    User findByUsername(String username);

    @Select("SELECT id, username, password, nickname, avatar, status, created_at, updated_at " +
            "FROM `sys_user` WHERE id = #{userId} LIMIT 1")
    User findByUserId(Long userId);

    @Insert("INSERT INTO `sys_user` (username, password, nickname, avatar, status, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{avatar}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    @Update("UPDATE `sys_user` SET nickname = #{nickname}, avatar = #{avatar},updated_at = NOW() WHERE id = #{userId}")
    int updateUserInfo(Long userId, String nickname, String avatar);

    @Delete("DELETE FROM `sys_user` WHERE id = #{userId}")
    int delete(@Param("userId") Long userId);
}
