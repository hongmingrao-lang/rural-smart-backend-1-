package com.ruralsmart.impl;

import com.ruralsmart.dto.LoginDTO;
import com.ruralsmart.dto.RegisterDTO;
import com.ruralsmart.entity.User;
import com.ruralsmart.repository.UserRepository;
import com.ruralsmart.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User login(LoginDTO loginDTO, HttpSession session) {
        User user = userRepository.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!user.getPassword().equals(loginDTO.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        session.setAttribute("currentUser", user);
        log.info("用户登录成功: {}", user.getUsername());
        return user;
    }

    @Override
    public User register(RegisterDTO registerDTO) {
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(registerDTO.getPassword());
        user.setRole("user");

        User saved = userRepository.save(user);
        log.info("用户注册成功: {}", saved.getUsername());
        return saved;
    }

    @Override
    public void logout(HttpSession session) {
        Object user = session.getAttribute("currentUser");
        if (user != null) {
            log.info("用户登出: {}", ((User) user).getUsername());
        }
        session.invalidate();
    }

    @Override
    public User getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            throw new RuntimeException("未登录");
        }
        return user;
    }
}
