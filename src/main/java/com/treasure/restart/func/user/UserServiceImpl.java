package com.treasure.restart.func.user;

import com.treasure.restart.dto.UserLoginRequest;
import com.treasure.restart.dto.UserLoginResponse;
import com.treasure.restart.entity.User;
import com.treasure.restart.helper.JwtUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public UserLoginResponse loginPwd(UserLoginRequest request) {

        User existingUser = userMapper.findByUsername(request.getUsername());

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

        // 用户存在 → 验证密码
        boolean matches = passwordEncoder.matches(
                request.getPassword(),
                existingUser.getPassword()
        );

        if (!matches) {
            throw new IllegalArgumentException("密码错误");
        }

        return generateResponse(existingUser);
    }

    private static @NonNull UserLoginResponse generateResponse(User newUser) {
        String token = JwtUtils.generateToken(newUser.getId());
        UserLoginResponse response = new UserLoginResponse();
        response.setToken(token);
        response.setUserId(newUser.getId());
        response.setUsername(newUser.getUsername());
        response.setNickname(newUser.getNickname());
        return response;
    }
}
