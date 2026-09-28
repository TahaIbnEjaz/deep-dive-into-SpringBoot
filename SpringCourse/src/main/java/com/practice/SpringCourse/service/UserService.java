package com.practice.SpringCourse.service;

import com.practice.SpringCourse.model.Users;
import com.practice.SpringCourse.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public Users addUser(Users user) {

        user.setPassword(encoder.encode(user.getPassword()));

        return userRepo.save(user);
    }
}
