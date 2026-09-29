package com.practice.SpringCourse.repository;

import com.practice.SpringCourse.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<Users, Integer> {

    Users findByUserName(String username);
}
