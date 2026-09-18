package com.treasure.restart.func.user;

import com.treasure.restart.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("SELECT id, username, password, nickname, avatar, status, created_at, updated_at " +
            "FROM `sys_user` WHERE username = #{username} LIMIT 1")
    User findByUsername(String username);

    @Insert("INSERT INTO `sys_user` (username, password, nickname, avatar, status, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{avatar}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);
}
