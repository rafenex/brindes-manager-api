package br.com.rafael.brindesmanager.service;

import br.com.rafael.brindesmanager.dto.request.ProductRequest;
import br.com.rafael.brindesmanager.dto.response.ProductDropdownResponse;
import br.com.rafael.brindesmanager.dto.response.ProductResponse;
import br.com.rafael.brindesmanager.entity.Category;
import br.com.rafael.brindesmanager.entity.Product;
import br.com.rafael.brindesmanager.exception.BusinessException;
import br.com.rafael.brindesmanager.exception.NotFoundException;
import br.com.rafael.brindesmanager.repository.ProductRepository;
import br.com.rafael.brindesmanager.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Long companyId = currentUserService.getCurrentCompanyId();

        if (productRepository.existsByCompanyIdAndReferenceIgnoreCase(
                companyId,
                request.reference()
        )) {
            throw new BusinessException("Já existe um produto com essa referência");
        }

        Category category =
                categoryService.findActiveCategoryById(request.categoryId());

        Product product = Product.builder()
                .reference(request.reference())
                .name(request.name())
                .description(request.description())
                .basePrice(request.basePrice())
                .category(category)
                .company(category.getCompany())
                .build();

        Product savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(Long categoryId) {
        Long companyId = currentUserService.getCurrentCompanyId();

        List<Product> products;

        if (categoryId != null) {
            products = productRepository
                    .findAllByCompanyIdAndCategoryIdAndActiveTrueOrderByNameAsc(
                            companyId,
                            categoryId
                    );
        } else {
            products = productRepository
                    .findAllByCompanyIdAndActiveTrueOrderByNameAsc(companyId);
        }

        return products.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = findActiveProductById(id);

        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Long companyId = currentUserService.getCurrentCompanyId();

        Product product = findActiveProductById(id);

        boolean referenceChanged =
                !product.getReference().equalsIgnoreCase(request.reference());

        if (referenceChanged &&
                productRepository.existsByCompanyIdAndReferenceIgnoreCase(
                        companyId,
                        request.reference()
                )) {
            throw new BusinessException("Já existe um produto com essa referência");
        }

        Category category =
                categoryService.findByCategoryId(request.categoryId());

        product.setReference(request.reference());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setBasePrice(request.basePrice());
        product.setCategory(category);
        product.setUpdatedAt(LocalDateTime.now());

        Product updatedProduct = productRepository.save(product);

        return toResponse(updatedProduct);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findActiveProductById(id);

        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);
    }

    public Product findActiveProductById(Long id) {
        Long companyId = currentUserService.getCurrentCompanyId();

        return productRepository
                .findByIdAndCompanyIdAndActiveTrue(id, companyId)
                .orElseThrow(() ->
                        new NotFoundException("Produto não encontrado")
                );
    }

    @Transactional(readOnly = true)
    public List<ProductDropdownResponse> findAllDropdown() {
        Long companyId = currentUserService.getCurrentCompanyId();

        return productRepository
                .findAllByCompanyIdOrderByNameAsc(companyId)
                .stream()
                .map(product -> new ProductDropdownResponse(
                        product.getId(),
                        product.getReference(),
                        product.getName(),
                        product.getBasePrice(),
                        product.getActive()
                ))
                .toList();
    }

    public Product findByProductId(Long id) {
        Long companyId = currentUserService.getCurrentCompanyId();

        return productRepository
                .findByIdAndCompanyId(id, companyId)
                .orElseThrow(() ->
                        new NotFoundException("Produto não encontrado")
                );
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getReference(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getActive(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}