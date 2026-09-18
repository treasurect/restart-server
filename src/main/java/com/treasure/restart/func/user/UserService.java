package com.treasure.restart.func.user;

import com.treasure.restart.dto.UserLoginRequest;
import com.treasure.restart.dto.UserLoginResponse;

public interface UserService {

    UserLoginResponse loginPwd(UserLoginRequest request);
}
