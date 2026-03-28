package com.revshop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seller_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seller_seq_gen")
    @SequenceGenerator(name = "seller_seq_gen", sequenceName = "seller_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 150)
    private String businessName;

    @Column(length = 20)
    private String gstNumber;

    @Column(length = 15)
    private String phone;

    @Column(length = 255)
    private String businessAddress;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}