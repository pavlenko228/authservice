package com.bank.authservice.service.impl;

import java.util.List;


import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bank.authservice.domain.dto.AuthenticationResponse;
import com.bank.authservice.domain.dto.LoginRequest;
import com.bank.authservice.domain.dto.RegistrationRequest;
import com.bank.authservice.domain.model.Role;
import com.bank.authservice.domain.model.Token;
import com.bank.authservice.domain.model.User;
import com.bank.authservice.exception.InvalidRefreshTokenException;
import com.bank.authservice.exception.MissingAuthorizationHeaderException;
import com.bank.authservice.exception.SuchUserExistsException;
import com.bank.authservice.service.contract.AuthenticationService;
import com.bank.authservice.service.contract.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import com.bank.authservice.repository.TokenRepository;
import com.bank.authservice.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;

    private final TokenRepository tokenRepository;

    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;
    
    public AuthenticationResponse register(RegistrationRequest registrationRequest, HttpServletResponse response) {
        User user = User.builder()
                        .id(null)
                        .username(registrationRequest.getUsername())
                        .email(registrationRequest.getEmail())
                        .phone(registrationRequest.getPhone())
                        .password(passwordEncoder.encode(registrationRequest.getPassword()))  
                        .role(Role.ROLE_USER)
                        .build();

        if (!userRepository.existsByEmail(user.getEmail()) && !userRepository.existsByPhone(user.getPhone())) { 

            userRepository.save(user); 

            String accessToken = jwtService.generateAccessToken(user); 
            String refreshToken = jwtService.generateRefreshToken(user);

            saveUserToken(accessToken, refreshToken, user);  

            response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            response.setHeader("X-Refresh-Token", refreshToken);
        
            return new AuthenticationResponse(accessToken, refreshToken);
        }
        
        throw new SuchUserExistsException("An user with such an email or phone number has already been created");
    }

    public AuthenticationResponse authenticate(LoginRequest loginRequest, HttpServletResponse response) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(),
                loginRequest.getPassword()
            )
        ); 
        
        User user = userRepository.findByEmail(loginRequest.getEmail()).get(); 

        String accessToken = jwtService.generateAccessToken(user); 
        String refreshToken = jwtService.generateRefreshToken(user);

        revokeAllToken(user); 

        saveUserToken(accessToken, refreshToken, user);

        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        response.setHeader("X-Refresh-Token", refreshToken);

        return new AuthenticationResponse(accessToken, refreshToken);
    }

    public AuthenticationResponse refreshToken(HttpServletRequest request, HttpServletResponse response) throws MissingAuthorizationHeaderException {

        String token = request.getHeader("X-Refresh-Token");

        if (token == null) {
            throw new MissingAuthorizationHeaderException("User unauthenticate");
        }
        
        String email = jwtService.extractEmail(token);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found")); 

        if (jwtService.isRefreshTokenValid(token, user)) {
            
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            revokeAllToken(user);

            saveUserToken(accessToken, refreshToken, user);

            response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            response.setHeader("X-Refresh-Token", refreshToken);

            return new AuthenticationResponse(accessToken, refreshToken);
        }

        throw new InvalidRefreshTokenException("Token is invalid");
    }

    public void revokeAllToken(User user) {
        List<Token> validTokens = tokenRepository.findAllTokenByUser(user.getId());
      
        if (!validTokens.isEmpty()) {
          validTokens.forEach(t -> t.setLoggedOut(true));
        }
        
        tokenRepository.saveAll(validTokens);
    }

    public void saveUserToken(String accessToken, String refreshToken, User user) {
        tokenRepository.save(
            Token.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .loggedOut(false)
                .user(user)
                .build()
        );
    }

}