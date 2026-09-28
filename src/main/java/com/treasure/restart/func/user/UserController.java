package com.treasure.restart.func.user;

import com.treasure.restart.base.BaseResponse;
import com.treasure.restart.dto.UserInfoResponse;
import com.treasure.restart.dto.UserLoginRequest;
import com.treasure.restart.dto.UserLoginResponse;
import com.treasure.restart.dto.UserUpdateRequest;
import com.treasure.restart.helper.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/loginPwd")
    public BaseResponse<UserLoginResponse> loginPwd(@Valid @RequestBody UserLoginRequest request) {
        try {
            UserLoginResponse data = userService.loginPwd(request);
            return BaseResponse.success("登录成功",data);
        } catch (IllegalArgumentException e) {
            return BaseResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/info")
    public BaseResponse<UserInfoResponse> info() {
        UserInfoResponse userInfo = userService.getUserInfo();
        return BaseResponse.success("用户信息获取成功" , userInfo);
    }

    @PutMapping("/update")
    public BaseResponse<Boolean> update(@Valid @RequestBody UserUpdateRequest request) {
        Boolean updated = userService.updateUserInfo(request);
        return BaseResponse.success("用户信息修改成功", updated);
    }

    @DeleteMapping("/delete")
    public BaseResponse<Boolean> delete() {
        Boolean result = userService.delete();
        return BaseResponse.success("删除成功",result);
    }

}
