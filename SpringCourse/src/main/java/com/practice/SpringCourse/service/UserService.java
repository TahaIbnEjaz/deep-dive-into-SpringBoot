package com.practice.SpringCourse.service;

import com.practice.SpringCourse.model.UserPrincipal;
import com.practice.SpringCourse.model.Users;
import com.practice.SpringCourse.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JWTService jwtService;

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public Users addUser(Users user) {

        user.setPassword(encoder.encode(user.getPassword()));

        return userRepo.save(user);
    }

    public String verify(Users user) {
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUserName(), user.getPassword())
            );

            if (authentication.isAuthenticated()) {
                return jwtService.generateToken(user.getUserName());
            }
        } catch (AuthenticationException e) {
            System.out.println(e.getCause());
            Users user1 = userRepo.findByUserName(user.getUserName());
            System.out.println(user1);
            return "failed";
        }

        return "failed";
    }

    public Users remove(int id) {
        Users user = userRepo.findById(id).orElseThrow(IllegalArgumentException::new);

        userRepo.delete(user);

        return user;
    }
}
