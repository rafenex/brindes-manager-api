package br.com.rafael.brindesmanager.security;

import br.com.rafael.brindesmanager.entity.AppUser;
import br.com.rafael.brindesmanager.exception.BusinessException;
import br.com.rafael.brindesmanager.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {
            throw new BusinessException("Usuário não autenticado");
        }

        return appUserRepository.findByIdAndActiveTrue(customUserDetails.getId())
                .orElseThrow(() -> new BusinessException("Usuário autenticado não encontrado"));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}