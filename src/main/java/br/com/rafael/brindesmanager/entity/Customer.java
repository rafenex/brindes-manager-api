package br.com.rafael.brindesmanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(name = "postal_code", columnDefinition = "TEXT")
    private String postalCode;

    @Column(name = "street", columnDefinition = "TEXT")
    private String street;

    @Column(name = "address_number", columnDefinition = "TEXT")
    private String number;

    @Column(name = "address_complement", columnDefinition = "TEXT")
    private String complement;

    @Column(name = "neighborhood", columnDefinition = "TEXT")
    private String neighborhood;

    @Column(name = "city", columnDefinition = "TEXT")
    private String city;

    @Column(name = "state", columnDefinition = "TEXT")
    private String state;

    @Column(length = 30)
    private String document;

    @Column(length = 160)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false)
    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @PrePersist
    public void prePersist() {
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }
}