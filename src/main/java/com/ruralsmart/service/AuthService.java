package com.ruralsmart.service;

import com.ruralsmart.dto.LoginDTO;
import com.ruralsmart.dto.RegisterDTO;
import com.ruralsmart.entity.User;

import javax.servlet.http.HttpSession;

public interface AuthService {

    User login(LoginDTO loginDTO, HttpSession session);

    User register(RegisterDTO registerDTO);

    void logout(HttpSession session);

    User getCurrentUser(HttpSession session);
}
