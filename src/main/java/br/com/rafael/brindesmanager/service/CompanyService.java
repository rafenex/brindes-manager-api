package br.com.rafael.brindesmanager.service;

import br.com.rafael.brindesmanager.dto.request.UpdateCompanyRequest;
import br.com.rafael.brindesmanager.dto.response.CompanyResponse;
import br.com.rafael.brindesmanager.entity.Company;
import br.com.rafael.brindesmanager.exception.BusinessException;
import br.com.rafael.brindesmanager.repository.CompanyRepository;
import br.com.rafael.brindesmanager.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024; // 2 MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private final CompanyRepository companyRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public CompanyResponse findCurrentCompany() {
        Company company = getCurrentCompany();

        return toResponse(company);
    }

    @Transactional
    public CompanyResponse update(UpdateCompanyRequest request) {
        Company company = getCurrentCompany();

        company.setName(request.name());
        company.setAddress(request.address());
        company.setEmail(request.email());
        company.setPhone(request.phone());

        Company updatedCompany = companyRepository.save(company);

        return toResponse(updatedCompany);
    }

    @Transactional
    public void updateLogo(MultipartFile file) {
        validateLogo(file);

        Company company = getCurrentCompany();

        try {
            company.setLogo(file.getBytes());
            company.setLogoContentType(file.getContentType());

            companyRepository.save(company);
        } catch (IOException ex) {
            throw new BusinessException("Não foi possível salvar a logo da empresa");
        }
    }

    @Transactional(readOnly = true)
    public byte[] getLogo() {
        Company company = getCurrentCompany();

        if (company.getLogo() == null || company.getLogo().length == 0) {
            throw new BusinessException("Empresa não possui logo cadastrada");
        }

        return company.getLogo();
    }

    @Transactional(readOnly = true)
    public String getLogoContentType() {
        Company company = getCurrentCompany();

        return company.getLogoContentType();
    }

    @Transactional
    public void deleteLogo() {
        Company company = getCurrentCompany();

        company.setLogo(null);
        company.setLogoContentType(null);

        companyRepository.save(company);
    }

    private Company getCurrentCompany() {
        return currentUserService
                .getCurrentUser()
                .getCompany();
    }

    private void validateLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("A logo é obrigatória");
        }

        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new BusinessException("A logo deve ter no máximo 2 MB");
        }

        if (file.getContentType() == null
                || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {

            throw new BusinessException(
                    "Formato de logo inválido. Utilize PNG, JPG ou WEBP"
            );
        }
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getAddress(),
                company.getEmail(),
                company.getPhone(),
                company.getLogo() != null
                        && company.getLogo().length > 0
        );
    }
}