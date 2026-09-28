package com.treasure.restart.func.user;

import com.treasure.restart.dto.UserInfoResponse;
import com.treasure.restart.dto.UserLoginRequest;
import com.treasure.restart.dto.UserLoginResponse;
import com.treasure.restart.dto.UserUpdateRequest;

public interface UserService {

    UserLoginResponse loginPwd(UserLoginRequest request);

    UserInfoResponse getUserInfo();

    Boolean updateUserInfo(UserUpdateRequest request);

    Boolean delete();

    Boolean delete(Long id);
}
