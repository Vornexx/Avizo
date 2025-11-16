package org.vornex.auth.util;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterWrapper {
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();


    /**
     * Получаем Bucket для ключа. Если нет — создаем новый с указанной емкостью и refill.
     *
     * @param key            уникальный ключ (IP, userId, API key)
     * @param capacity       максимальное количество токенов
     * @param refillAmount   сколько токенов добавлять при пополнении
     * @param refillDuration интервал пополнения
     * @return Bucket
     */
    public Bucket resolveBucket(String key, long capacity, long refillAmount, Duration refillDuration) {
        return cache.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(
                    capacity,
                    Refill.greedy(refillAmount, refillDuration)
            );
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    /**
     * Попытка "съесть" один токен из Bucket для ключа.
     *
     * @param key            уникальный ключ
     * @param capacity       максимальное количество токенов
     * @param refillAmount   сколько токенов добавлять при пополнении
     * @param refillDuration интервал пополнения
     * @return true, если токен доступен, false если лимит исчерпан
     */
    public boolean tryConsume(String key, long capacity, long refillAmount, Duration refillDuration) {
        Bucket bucket = resolveBucket(key, capacity, refillAmount, refillDuration);
        boolean allowed = bucket.tryConsume(1);
        System.out.println("Ключ: " + key + ", разрешено: " + allowed + ", оставшиеся токены: " + bucket.getAvailableTokens());
        return allowed;
    }

}
