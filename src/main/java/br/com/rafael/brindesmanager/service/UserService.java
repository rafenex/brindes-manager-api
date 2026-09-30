package br.com.rafael.brindesmanager.service;

import br.com.rafael.brindesmanager.dto.request.CreateUserRequest;
import br.com.rafael.brindesmanager.dto.response.UserResponse;
import br.com.rafael.brindesmanager.entity.AppUser;
import br.com.rafael.brindesmanager.enums.UserRole;
import br.com.rafael.brindesmanager.exception.BusinessException;
import br.com.rafael.brindesmanager.repository.AppUserRepository;
import br.com.rafael.brindesmanager.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {

        if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("Já existe um usuário com esse e-mail");
        }

        AppUser currentUser = currentUserService.getCurrentUser();

        AppUser user = AppUser.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .company(currentUser.getCompany())
                .build();

        AppUser savedUser = appUserRepository.save(user);

        return toResponse(savedUser);
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getActive()
        );
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        Long companyId = currentUserService.getCurrentCompanyId();

        return appUserRepository
                .findAllByCompanyIdOrderByNameAsc(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }
}