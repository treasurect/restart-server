package com.treasure.restart.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
