package com.ecommerce.user.service;

import com.ecommerce.user.dto.UserRegistrationRequest;
import com.ecommerce.user.dto.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse registerUser(UserRegistrationRequest request);

    UserResponse loginUser(com.ecommerce.user.dto.UserLoginRequest request);

    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    void incrementOrderCount(Long userId);

    UserResponse updateUser(Long id, com.ecommerce.user.dto.UserRegistrationRequest request);

    void deleteUser(Long id);
}
