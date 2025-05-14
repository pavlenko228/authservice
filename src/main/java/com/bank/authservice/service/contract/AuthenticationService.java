package com.bank.authservice.service.contract;

import com.bank.authservice.domain.dto.AuthenticationResponse;
import com.bank.authservice.domain.dto.LoginRequest;
import com.bank.authservice.domain.dto.RegistrationRequest;
import com.bank.authservice.domain.model.User;
import com.bank.authservice.exception.MissingAuthorizationHeaderException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthenticationService {

    AuthenticationResponse register(RegistrationRequest registrationRequest, HttpServletResponse response);

    AuthenticationResponse authenticate(LoginRequest loginRequest, HttpServletResponse response);

    AuthenticationResponse refreshToken(HttpServletRequest request, HttpServletResponse response) throws MissingAuthorizationHeaderException;

    void revokeAllToken(User user);

    void saveUserToken(String accessToken, String refreshToken, User user);
}