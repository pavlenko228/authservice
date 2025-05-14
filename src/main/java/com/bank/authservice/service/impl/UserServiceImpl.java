package com.bank.authservice.service.impl;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bank.authservice.domain.model.SecurityUser;
import com.bank.authservice.repository.UserRepository;
import com.bank.authservice.service.contract.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public SecurityUser loadUserByUsername(String email) throws UsernameNotFoundException {
        return SecurityUser.fromUser(userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found")));
    }
    
}
