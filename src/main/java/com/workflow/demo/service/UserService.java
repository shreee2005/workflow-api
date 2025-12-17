package com.workflow.demo.service;

import com.workflow.demo.dto.CreateUserDto;
import com.workflow.demo.entity.User;
import com.workflow.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    public UserRepository userRepository;
    public PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String lowerCase, String password, String name) {
        if(userRepository.existsByEmail(lowerCase)){
            throw new IllegalArgumentException("Email Already Exists !!");
        }

        User user = new User();
        user.setEmail(lowerCase);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
    public User createUserFromOAuth(String email, String name) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).get();
        }
        User u = new User();
        u.setEmail(email);
        u.setName(name);
        // no password for OAuth users — optionally set a random string
        u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        return userRepository.save(u);
    }
    public User createOrLinkOAuthUser(String provider, String providerId, String email, String name, boolean emailVerified) {

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            if (provider.equals(existing.getOauthProvider()) && providerId.equals(existing.getOauthId())) {
                return existing;
            }

            if (existing.getOauthProvider() == null && emailVerified) {
                existing.setOauthProvider(provider);
                existing.setOauthId(providerId);
                return userRepository.save(existing);
            }

            return existing;
        }

        User u = new User();
        u.setEmail(email);
        u.setName(name);
        u.setOauthProvider(provider);
        u.setOauthId(providerId);
        u.setPassword(null);
        return userRepository.save(u);
    }

}
