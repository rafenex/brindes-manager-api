package br.com.rafael.brindesmanager.service;

import br.com.rafael.brindesmanager.dto.request.CreateUserRequest;
import br.com.rafael.brindesmanager.dto.request.UpdateUserRequest;
import br.com.rafael.brindesmanager.dto.response.UserResponse;
import br.com.rafael.brindesmanager.entity.AppUser;
import br.com.rafael.brindesmanager.enums.UserRole;
import br.com.rafael.brindesmanager.exception.BusinessException;
import br.com.rafael.brindesmanager.exception.NotFoundException;
import br.com.rafael.brindesmanager.repository.AppUserRepository;
import br.com.rafael.brindesmanager.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        return toResponse(appUserRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return appUserRepository
                .findAllByCompanyIdAndDeletedAtIsNullOrderByNameAsc(currentUserService.getCurrentCompanyId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findForEdit(Long id) {
        AppUser actor = currentUserService.getCurrentUser();
        verifyAdministrator(actor, id);
        return appUserRepository.findByIdAndCompanyIdAndDeletedAtIsNull(id, actor.getCompany().getId())
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Conta de usuário indisponível"));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        AppUser target = lockTarget(id);
        if (appUserRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new BusinessException("Já existe um usuário com esse e-mail");
        }
        target.setName(request.name());
        target.setEmail(request.email());
        target.setUpdatedAt(LocalDateTime.now());
        return toResponse(target);
    }

    @Transactional
    public void delete(Long id) {
        AppUser target = lockTarget(id);
        LocalDateTime deletedAt = LocalDateTime.now();
        target.setDeletedAt(deletedAt);
        target.setActive(false);
        target.setUpdatedAt(deletedAt);
    }

    private AppUser lockTarget(Long id) {
        var principal = currentUserService.getAuthenticatedPrincipal();
        verifyAdministrator(principal.getUser(), id);

        List<AppUser> locked = appUserRepository.lockForManagement(principal.getCompanyId(), principal.getId(), id);
        AppUser actor = locked.stream()
                .filter(user -> user.getId().equals(principal.getId()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Acesso à gestão de usuários negado"));
        verifyAdministrator(actor, id);

        return locked.stream()
                .filter(user -> user.getId().equals(id) && user.getDeletedAt() == null)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Conta de usuário indisponível"));
    }

    private void verifyAdministrator(AppUser actor, Long targetId) {
        if (!Boolean.TRUE.equals(actor.getActive()) || actor.getRole() != UserRole.ADMIN || actor.getId().equals(targetId)) {
            throw new AccessDeniedException("Acesso à gestão desta conta negado");
        }
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
}
