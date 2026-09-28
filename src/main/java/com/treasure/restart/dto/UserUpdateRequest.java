package com.treasure.restart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50,message = "昵称的长度不能超过50")
    private String nickname;

    private String avatar;
}
