package br.com.rafael.brindesmanager.service;

import br.com.rafael.brindesmanager.dto.request.LoginRequest;
import br.com.rafael.brindesmanager.dto.request.RegisterRequest;
import br.com.rafael.brindesmanager.dto.response.AuthResponse;
import br.com.rafael.brindesmanager.entity.AppUser;
import br.com.rafael.brindesmanager.entity.Company;
import br.com.rafael.brindesmanager.enums.UserRole;
import br.com.rafael.brindesmanager.exception.BusinessException;
import br.com.rafael.brindesmanager.repository.AppUserRepository;
import br.com.rafael.brindesmanager.repository.CompanyRepository;
import br.com.rafael.brindesmanager.security.CustomUserDetails;
import br.com.rafael.brindesmanager.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CompanyRepository companyRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("Já existe um usuário com esse e-mail");
        }

        Company company = Company.builder()
                .name(request.companyName())
                .build();

        company = companyRepository.save(company);

        AppUser user = AppUser.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.ADMIN)
                .company(company)
                .build();

        AppUser savedUser = appUserRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        AppUser user = appUserRepository.findByEmailIgnoreCaseAndActiveTrue(request.email())
                .orElseThrow(() -> new BusinessException("E-mail ou senha inválidos"));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}