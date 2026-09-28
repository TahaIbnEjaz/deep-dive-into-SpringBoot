package com.practice.SpringCourse.service;

import com.practice.SpringCourse.model.UserPrincipal;
import com.practice.SpringCourse.model.Users;
import com.practice.SpringCourse.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Users user = userRepo.findByUserName(username);

        if (user == null){
            throw new UsernameNotFoundException("User Not Found");
        }
        return new UserPrincipal(user);
    }
}
