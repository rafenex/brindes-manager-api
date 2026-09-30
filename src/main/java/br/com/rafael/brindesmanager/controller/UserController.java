package br.com.rafael.brindesmanager.controller;

import br.com.rafael.brindesmanager.dto.request.CreateUserRequest;
import br.com.rafael.brindesmanager.dto.response.UserResponse;
import br.com.rafael.brindesmanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(
            @RequestBody @Valid CreateUserRequest request
    ) {
        return userService.create(request);
    }
}