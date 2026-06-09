package com.enterprise.payment.repositories;

import com.enterprise.payment.entities.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    /**
     * WHAT IT DOES: Looks up a stored idempotency key by its value.
     * WHAT HAPPENS: IdempotencyService calls this when Redis misses.
     * If found and not expired, returns the cached response.
     * If the key expired (expiresAt is in the past), returns empty.
     * The .filter() in IdempotencyService handles expiry check.
     */
    Optional<IdempotencyKey> findByKeyValue(String keyValue);

    /**
     * WHAT IT DOES: Deletes all expired idempotency keys.
     * WHAT HAPPENS: Scheduled job calls this nightly.
     * Without cleanup, the idempotency_keys table grows forever.
     * @Modifying executes a single DELETE SQL — much faster than
     * loading entities and calling deleteAll().
     */
    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt < :now")
    int deleteExpiredKeys(Instant now);

}
