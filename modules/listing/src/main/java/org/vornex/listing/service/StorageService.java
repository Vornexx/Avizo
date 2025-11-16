package org.vornex.listing.service;


import org.vornex.exception.NotFoundException;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

public interface StorageService {

    /**
     * Результат presign PUT: URL и время жизни.
     */
    record PresignResult(String url, Instant expiresAt) {
    }

    /**
     * Выпускает presigned PUT URL, привязанный к конкретному key и contentType.
     * Подпись должна ограничивать именно этот key и требуемый Content-Type (если провайдер поддерживает).
     */
    PresignResult presignPut(String key, String contentType, Duration ttl);

    /**
     * HEAD-подобная информация: content length и content type.
     */
    record HeadResult(long contentLength, String contentType) {
    }

    HeadResult head(String key) throws NotFoundException;

    /**
     * Возвращает InputStream с началом объекта (для быстрой проверки магических байт/ImageIO).
     * В реализации обязателен лимит чтения.
     */
    InputStream getRange(String key, long maxBytes) throws NotFoundException;

    /**
     * Удаляет объект в хранилище (безопасно).
     */
    void delete(String key);

    /**
     * Построить публичный URL (через CDN домен / собственный endpoint).
     * Реализация может конкатенировать cdnDomain + "/" + key.
     */
    String publicUrl(String key);
}
