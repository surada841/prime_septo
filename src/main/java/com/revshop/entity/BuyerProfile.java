package com.revshop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "buyer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyerProfile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "buyer_seq_gen")
    @SequenceGenerator(name = "buyer_seq_gen", sequenceName = "buyer_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(length = 15)
    private String phone;

    @Column(length = 255)
    private String address;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}