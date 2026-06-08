package com.enterprise.payment.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
@Entity
@Table(name = "idempotency_keys",indexes = {
        @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String keyValue;

    private String response;


    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", updatable = false,nullable = false)
    private Instant expiresAt;



}
