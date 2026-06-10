package com.enterprise.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * WHAT IT DOES: Creates a typed RedisTemplate<String, String>.
     *
     * WHAT HAPPENS:
     *   - Key serializer   = StringRedisSerializer
     *     Keys are stored as plain UTF-8 strings (e.g. "idempotency:pay_ORD001_abc")
     *     Human-readable in Redis CLI — critical for debugging.
     *
     *   - Value serializer = StringRedisSerializer
     *     Values stored as JSON strings (serialized by IdempotencyService).
     *     Using String instead of Java serialization means data survives
     *     application restarts and class refactors without corruption.
     *
     *   Without custom serializers, Spring uses JdkSerializationRedisSerializer
     *   which produces binary blobs that are unreadable in Redis CLI
     *   and break when class structures change.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Keys: plain strings — readable in Redis CLI
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Values: JSON strings for human readability + resilience
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * WHAT IT DOES: Creates a configured ObjectMapper for JSON serialization.
     *
     * WHAT HAPPENS:
     *   - JavaTimeModule: enables serialization of Java 8+ date/time types
     *     (Instant, LocalDateTime) as ISO-8601 strings, not timestamps.
     *   - WRITE_DATES_AS_TIMESTAMPS disabled: dates stored as
     *     "2024-01-15T10:30:00Z" not as "1705316200000".
     *   Used by IdempotencyService to serialize PaymentResponse to JSON
     *   before storing in Redis, and to deserialize on retrieval.
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
