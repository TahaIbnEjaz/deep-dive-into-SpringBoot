package com.practice.SpringCourse.controller;

import com.practice.SpringCourse.model.Users;
import com.practice.SpringCourse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Users registerUser(@RequestBody Users user){

        return userService.addUser(user);
    }

    @PostMapping("/login")
    public String login(@RequestBody Users user){
        return userService.verify(user);
    }

    @DeleteMapping("/deleteUser/{id}")
    public Users removeUser(@PathVariable int id){
        return userService.remove(id);
    }

}
