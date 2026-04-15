package com.ruralsmart.controller;

import com.ruralsmart.dto.LoginDTO;
import com.ruralsmart.dto.RegisterDTO;
import com.ruralsmart.entity.User;
import com.ruralsmart.service.AuthService;
import com.ruralsmart.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO, HttpSession session) {
        try {
            User user = authService.login(loginDTO, session);
            Map<String, Object> userVO = new HashMap<>();
            userVO.put("id", user.getId());
            userVO.put("username", user.getUsername());
            userVO.put("role", user.getRole());
            return Result.success("登录成功", userVO);
        } catch (RuntimeException e) {
            return Result.error(401, e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterDTO registerDTO) {
        try {
            User user = authService.register(registerDTO);
            Map<String, Object> userVO = new HashMap<>();
            userVO.put("id", user.getId());
            userVO.put("username", user.getUsername());
            userVO.put("role", user.getRole());
            return Result.success("注册成功", userVO);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        authService.logout(session);
        return Result.success("登出成功", null);
    }

    @GetMapping("/current")
    public Result<Map<String, Object>> getCurrentUser(HttpSession session) {
        try {
            User user = authService.getCurrentUser(session);
            Map<String, Object> userVO = new HashMap<>();
            userVO.put("id", user.getId());
            userVO.put("username", user.getUsername());
            userVO.put("role", user.getRole());
            return Result.success(userVO);
        } catch (RuntimeException e) {
            return Result.error(401, e.getMessage());
        }
    }
}
