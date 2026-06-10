package com.enterprise.payment.repositories;

import com.enterprise.payment.entities.User;

import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.lang.ScopedValue;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
