package br.com.rafael.brindesmanager.service;

import br.com.rafael.brindesmanager.dto.request.CategoryRequest;
import br.com.rafael.brindesmanager.dto.response.CategoryDropdownResponse;
import br.com.rafael.brindesmanager.dto.response.CategoryResponse;
import br.com.rafael.brindesmanager.entity.AppUser;
import br.com.rafael.brindesmanager.entity.Category;
import br.com.rafael.brindesmanager.exception.BusinessException;
import br.com.rafael.brindesmanager.exception.NotFoundException;
import br.com.rafael.brindesmanager.repository.CategoryRepository;
import br.com.rafael.brindesmanager.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        AppUser user = currentUserService.getCurrentUser();
        Long companyId = user.getCompany().getId();

        if (categoryRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.name())) {
            throw new BusinessException("Já existe uma categoria com esse nome");
        }

        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .company(user.getCompany())
                .build();

        Category savedCategory = categoryRepository.save(category);

        return toResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        Long companyId = currentUserService.getCurrentCompanyId();

        return categoryRepository
                .findAllByCompanyIdAndActiveTrueOrderByNameAsc(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        Category category = findActiveCategoryById(id);

        return toResponse(category);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Long companyId = currentUserService.getCurrentCompanyId();

        Category category = findActiveCategoryById(id);

        boolean nameChanged =
                !category.getName().equalsIgnoreCase(request.name());

        if (nameChanged &&
                categoryRepository.existsByCompanyIdAndNameIgnoreCase(
                        companyId,
                        request.name()
                )) {
            throw new BusinessException("Já existe uma categoria com esse nome");
        }

        category.setName(request.name());
        category.setDescription(request.description());
        category.setUpdatedAt(LocalDateTime.now());

        Category updatedCategory = categoryRepository.save(category);

        return toResponse(updatedCategory);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findActiveCategoryById(id);

        category.setActive(false);
        category.setUpdatedAt(LocalDateTime.now());

        categoryRepository.save(category);
    }

    public Category findActiveCategoryById(Long id) {
        Long companyId = currentUserService.getCurrentCompanyId();

        return categoryRepository
                .findByIdAndCompanyIdAndActiveTrue(id, companyId)
                .orElseThrow(() ->
                        new NotFoundException("Categoria não encontrada")
                );
    }

    @Transactional(readOnly = true)
    public List<CategoryDropdownResponse> findAllDropdown() {
        Long companyId = currentUserService.getCurrentCompanyId();

        return categoryRepository
                .findAllByCompanyIdOrderByNameAsc(companyId)
                .stream()
                .map(category -> new CategoryDropdownResponse(
                        category.getId(),
                        category.getName(),
                        category.getActive()
                ))
                .toList();
    }

    public Category findByCategoryId(Long id) {
        Long companyId = currentUserService.getCurrentCompanyId();

        return categoryRepository
                .findByIdAndCompanyId(id, companyId)
                .orElseThrow(() ->
                        new NotFoundException("Categoria não encontrada")
                );
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}