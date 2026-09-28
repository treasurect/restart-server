package com.treasure.restart.func.user;

import com.treasure.restart.dto.UserInfoResponse;
import com.treasure.restart.dto.UserLoginRequest;
import com.treasure.restart.dto.UserLoginResponse;
import com.treasure.restart.dto.UserUpdateRequest;
import com.treasure.restart.entity.User;
import com.treasure.restart.helper.JwtUtils;
import com.treasure.restart.helper.UserContext;
import com.treasure.restart.helper.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtils jwtUtils;

    @Override
    public UserLoginResponse loginPwd(UserLoginRequest request) {

        User existingUser = userMapper.findByUsername(request.getUsername());

        // 用户不存在 → 注册并登录
        if (existingUser == null) {

            User newUser = new User();

            newUser.setUsername(request.getUsername());

            newUser.setPassword(
                    passwordEncoder.encode(request.getPassword())
            );

            newUser.setNickname(request.getNickname());

            newUser.setStatus(1);

            userMapper.insertUser(newUser);

            return generateResponse(newUser);
        }

        // 用户存在 → 检查用户状态
        if (existingUser.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 验证密码
        boolean matches = passwordEncoder.matches(
                request.getPassword(),
                existingUser.getPassword()
        );

        if (!matches) {
            throw new BusinessException("密码错误");
        }

        return generateResponse(existingUser);
    }

    @Override
    public UserInfoResponse getUserInfo() {
        Long userId = UserContext.getUserId();
        User curentUser = userMapper.findByUserId(userId);
        if (curentUser == null) {
            throw new BusinessException(404,"用户不存在");
        }
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(curentUser.getId());
        response.setUsername(curentUser.getUsername());
        response.setNickname(curentUser.getNickname());
        response.setAvatar(curentUser.getAvatar());
        return response;
    }

    @Override
    public Boolean updateUserInfo(UserUpdateRequest request) {
        Long userId = UserContext.getUserId();
        int rows = userMapper.updateUserInfo(userId, request.getNickname(), request.getAvatar());
        if (rows == 0) throw new BusinessException(404,"用户不存在");

        return true;
    }

    @Override
    public Boolean delete() {
        return delete(UserContext.getUserId());
    }

    @Override
    public Boolean delete(Long userId) {
        int rows = userMapper.delete(userId);
        if (rows == 0) {
            throw new BusinessException(404,"用户不存在");
        }
        return true;
    }


    private UserLoginResponse generateResponse(User user) {

        String token = jwtUtils.generateToken(user.getId());

        UserLoginResponse response = new UserLoginResponse();

        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());

        return response;
    }
}