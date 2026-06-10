package com.enterprise.payment.services.servicesImp;

import com.enterprise.payment.dtos.RegisterRequest;
import com.enterprise.payment.dtos.RegisterResponse;
import com.enterprise.payment.entities.User;
import com.enterprise.payment.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;

import static com.enterprise.payment.entities.Role.USER;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public RegisterResponse register(@Valid RegisterRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(request.getUsername())
                            .password(request.getPassword())
                            .email(request.getEmail())// In real app, hash this!
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .role(Collections.singletonList(USER))
                            .build();
                    return userRepository.save(newUser);
                });
        return modelMapper.map(user, RegisterResponse.class);

    }

}
