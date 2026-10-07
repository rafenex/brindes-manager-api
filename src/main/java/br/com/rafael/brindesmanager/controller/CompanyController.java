package br.com.rafael.brindesmanager.controller;

import br.com.rafael.brindesmanager.dto.request.UpdateCompanyRequest;
import br.com.rafael.brindesmanager.dto.response.CompanyResponse;
import br.com.rafael.brindesmanager.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/company/me")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public CompanyResponse findCurrentCompany() {
        return companyService.findCurrentCompany();
    }

    @PutMapping
    public CompanyResponse update(
            @RequestBody @Valid UpdateCompanyRequest request
    ) {
        return companyService.update(request);
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> getLogo() {
        byte[] logo = companyService.getLogo();
        String contentType = companyService.getLogoContentType();

        MediaType mediaType = contentType != null
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(logo);
    }

    @PutMapping(
            value = "/logo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> updateLogo(
            @RequestParam("file") MultipartFile file
    ) {
        companyService.updateLogo(file);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/logo")
    public ResponseEntity<Void> deleteLogo() {
        companyService.deleteLogo();

        return ResponseEntity.noContent().build();
    }
}