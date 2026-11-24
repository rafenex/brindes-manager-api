package br.com.rafael.brindesmanager.controller;

import br.com.rafael.brindesmanager.dto.response.PostalCodeResponse;
import br.com.rafael.brindesmanager.service.PostalCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/postal-codes")
@RequiredArgsConstructor
public class PostalCodeController {
    private final PostalCodeService postalCodeService;

    @GetMapping("/{postalCode}")
    public PostalCodeResponse find(@PathVariable String postalCode) {
        return postalCodeService.find(postalCode);
    }
}
