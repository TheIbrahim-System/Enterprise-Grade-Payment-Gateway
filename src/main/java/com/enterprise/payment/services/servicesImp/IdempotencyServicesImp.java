package com.enterprise.payment.services.servicesImp;

import com.enterprise.payment.dtos.PaymentResponse;
import com.enterprise.payment.entities.IdempotencyKey;
import com.enterprise.payment.repositories.IdempotencyKeyRepository;
import com.enterprise.payment.services.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServicesImp implements IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;
    private final IdempotencyKeyRepository repository;
    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);
    private final ModelMapper modelMapper;

    /**
     * WHAT IT DOES: Checks if this request was already processed.
     * WHAT HAPPENS:
     * 1. Looks up "idempotency:{key}" in Redis (microsecond lookup)
     * 2. If found → deserialize cached response and return it immediately
     * The caller knows this is a duplicate and skips processing.
     * 3. If not found in Redis → check DB (handles Redis restart/eviction)
     * 4. If not found anywhere → return empty Optional (new request)
     * This guarantees that even if the client retries 10 times,
     * Stripe is called only ONCE and the same response is returned.
     */


    @Override
    public Optional<PaymentResponse> getExistingResponse(String idempotencyKey){
        String cached = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
        if (cached != null) {
            log.warn("Idempotency key {} already exists in Redis. Overwriting.", idempotencyKey);
            return Optional.of(deserialize(cached));
        }
        return repository.findByKeyValue(idempotencyKey)
                .filter(k -> k.getExpiresAt().isAfter(Instant.now()))
                .map(k -> deserialize(k.getResponse()));

    }

    /**
     * WHAT IT DOES: Stores the response for this idempotency key.
     * WHAT HAPPENS: After a successful payment, we save the response
     * to both Redis (fast) and MySQL (durable) with a 24-hour TTL.
     * Any duplicate requests within 24 hours return this cached response.
     * After 24 hours, the key expires and the client must generate a new one.
     */
    @Override
    public void saveResponse(String idempotencyKey, PaymentResponse response) {
        String serialized = serialize(response);
        String redisKey = KEY_PREFIX + idempotencyKey;

        //save Redis value
        redisTemplate.opsForValue().set(redisKey, serialized, TTL);
        // save to db for duribility
        IdempotencyKey keyEntity = IdempotencyKey.builder()
                .keyValue(idempotencyKey)
                .response(serialized)
                .expiresAt(Instant.now().plus(TTL))
                .createdAt(Instant.now())
                .build();
        repository.save(keyEntity);
    }


    /**
     * WHAT IT DOES: Builds a deterministic idempotency key.
     * WHAT HAPPENS: Combines orderId + client nonce.
     * The nonce comes from the client and stays FIXED across retries
     * (unlike UUID.randomUUID() which would generate different keys).
     * This is the correct enterprise pattern — the client controls the key.
     */
    public String buildKey(String orderId, String nonce) {
        return "pay_" + orderId + "_" + nonce;
    }

    private String serialize(PaymentResponse r) {
        try { return modelMapper.map(r, String.class); }
        catch (Exception e) { throw new RuntimeException("Serialization failed", e); }
    }

    private PaymentResponse deserialize(String json) {
        try { return modelMapper.map(json, PaymentResponse.class); }
        catch (Exception e) { throw new RuntimeException("Deserialization failed", e); }
    }

}
