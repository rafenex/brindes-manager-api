package br.com.rafael.brindesmanager.service;

import br.com.rafael.brindesmanager.dto.request.CategoryRequest;
import br.com.rafael.brindesmanager.dto.response.CategoryResponse;
import br.com.rafael.brindesmanager.entity.Category;
import br.com.rafael.brindesmanager.exception.BusinessException;
import br.com.rafael.brindesmanager.exception.NotFoundException;
import br.com.rafael.brindesmanager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException("Já existe uma categoria com esse nome");
        }

        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();

        Category savedCategory = categoryRepository.save(category);

        return toResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc()
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
        Category category = findActiveCategoryById(id);

        boolean nameChanged = !category.getName().equalsIgnoreCase(request.name());

        if (nameChanged && categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException("Já existe uma categoria com esse nome");
        }

        category.setName(request.name());
        category.setDescription(request.description());

        Category updatedCategory = categoryRepository.save(category);

        return toResponse(updatedCategory);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findActiveCategoryById(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    private Category findActiveCategoryById(Long id) {
        return categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Categoria não encontrada"));
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