package com.seninsight.backend.auth;


import com.seninsight.backend.auth.dtos.LoginRequest;
import com.seninsight.backend.auth.dtos.LoginResponse;

public interface IAuthService {
    LoginResponse login(LoginRequest loginRequest);
}