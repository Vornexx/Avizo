package org.vornex.listing.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.vornex.exception.BadRequestException;
import org.vornex.exception.NotFoundException;
import org.vornex.listing.service.StorageService;
import org.vornex.listing.util.StorageProperties;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
public class S3StorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageServiceImpl.class);

    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProperties props;

    public S3StorageServiceImpl(S3Client s3, S3Presigner presigner, StorageProperties props) {
        this.s3 = Objects.requireNonNull(s3);
        this.presigner = Objects.requireNonNull(presigner);
        this.props = Objects.requireNonNull(props);
    }

    /**
     * Создаёт presigned PUT URL для конкретного ключа в бакете.
     * <p>
     * Шаги:
     * 1. Проверяет входные параметры (key и contentType). Если ttl некорректен — берёт дефолт из props.
     * 2. Строит PutObjectRequest с указанным contentType — это позволяет подписать требуемый
     * заголовок и затруднить заливку другого типа файла.
     * 3. Просит S3Presigner сгенерировать presigned URL с указанным TTL.
     * 4. Возвращает URL и момент истечения (expiresAt).
     * <p>
     * Важные замечания:
     * - URL привязан к конкретному key; по этой ссылке нельзя записать в другой key.
     * - Content-Type, указанный в подписи, должен совпадать с тем, что придёт при PUT.
     * - Не сохраняем presigned URL в БД — он временный и быстро "протухнет".
     */
    @Override
    public PresignResult presignPut(String key, String contentType, Duration ttl) {
        if (key == null || key.isBlank()) throw new BadRequestException("key is required for presign");
        if (contentType == null || contentType.isBlank())
            throw new BadRequestException("contentType is required for presign");
        if (ttl == null || ttl.isZero() || ttl.isNegative()) ttl = props.getPresignTtl();

        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                // Указываем content-type в подписи — клиент должен отправить такой же header при PUT.
                .contentType(contentType)
                // Не даём публичного ACL здесь — контролируем доступ через бакет/бразу CDN.
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(por)
                .signatureDuration(ttl)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);

        Instant expiresAt = Instant.now().plus(ttl);
        log.debug("Created presigned PUT for key={} expiresAt={}", key, expiresAt);
        return new PresignResult(presigned.url().toString(), expiresAt);
    }


    /**
     * Выполняет HEAD запроса к объекту в бакете и возвращает минимальные метаданные.
     * <p>
     * Шаги:
     * 1. Формирует HeadObjectRequest (bucket + key).
     * 2. Вызывает s3.headObject и маппит contentLength и contentType в HeadResult.
     * <p>
     * Обработка ошибок:
     * - Если объект не найден — кидает NotFoundException (приводит к 400/404 выше по стеку).
     * - Прочие S3Exception логируются как ошибка и пробрасываются (или можно завернуть в кастомное исключение).
     * <p>
     * Использование:
     * - Метод быстрый; подходит для валидации "наличия" объекта и сверки размера/типа.
     */
    @Override
    public HeadResult head(String key) {
        try {
            HeadObjectRequest req = HeadObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .build();
            HeadObjectResponse resp = s3.headObject(req);
            return new HeadResult(resp.contentLength(), resp.contentType());
        } catch (NoSuchKeyException e) {
            log.debug("Head request failed for key={} : {}", key, e.getMessage());
            throw new NotFoundException("Object not found: " + key);
        } catch (S3Exception e) {
            // тут может быть 403, 500 и т.д. — логируем как ошибку
            log.error("S3 error during head request for key={} : {}", key, e.awsErrorDetails().errorMessage(), e);
            throw e; // или завернуть в своё кастомное RuntimeException
        }
    }

    /**
     * Берёт первые maxBytes байт файла по ключу key в бакете и возвращает InputStream.
     * <p>
     * Шаги:
     * 1. Валидирует maxBytes (> 0).
     * 2. Формирует заголовок Range "bytes=0-{maxBytes-1}" и выполняет GetObject.
     * 3. Возвращает ResponseInputStream — **вызывающий обязан закрыть** поток (try-with-resources).
     * <p>
     * Поведение при ошибках:
     * - Если объект не найден — кидает NotFoundException.
     * - При S3 ошибок — логируем и пробрасываем (или можно завернуть).
     * <p>
     * Примечание:
     * - Метод используется для частичной загрузки (проверка magic-bytes / ImageIO).
     * - Не читает весь объект, поэтому безопасен для больших файлов при проверках.
     */

    @Override
    public InputStream getRange(String key, long maxBytes) {
        if (maxBytes <= 0) throw new BadRequestException("maxBytes must be > 0");
        String range = "bytes=0-" + (maxBytes - 1);
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .range(range)
                .build();
        try {
            // кто вызовет этот метод обязан закрыть стрим
            return s3.getObject(req);
        } catch (NoSuchKeyException e) {
            log.debug("getRange failed for key={} range={} : {}", key, range, e.getMessage());
            throw new NotFoundException("Object not found: " + key);
        } catch (S3Exception e) {
            log.error("S3 error while getting range for key={} range={} : {}", key, range, e.awsErrorDetails().errorMessage(), e);
            throw e; // или завернуть в своё исключение, если нужно
        }
    }

    /**
     * Удаляет объект по ключу в бакете.
     * <p>
     * Шаги:
     * 1. Формирует DeleteObjectRequest и вызывает s3.deleteObject.
     * <p>
     * Поведение при ошибках:
     * - Ошибки логируются, но не пробрасываются — удаление считается best-effort.
     * - Это важно для операций очистки: мы не хотим, чтобы фейл удаления ломал основной поток.
     */

    @Override
    public void delete(String key) {
        try {
            DeleteObjectRequest req = DeleteObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .build();
            s3.deleteObject(req);
        } catch (S3Exception e) {
            // логируем, но не кидаем, т.к. объект может быть уже удален, а нус будут много ретраев.
            log.warn("Failed to delete object {}: {}", key, e.getMessage());
        }
    }


    /**
     * Формирует публичный URL к объекту.
     * <p>
     * Логика:
     * 1. Если задан cdnDomain — возвращаем cdnDomain + "/" + key.
     * 2. Иначе — строим path-style URL: {endpoint}/{bucket}/{key}.
     * <p>
     * Замечания:
     * - URL сам по себе "постоянный" (не зависит от TTL CDN-кэша). Если кэш очистили,
     * CDN при следующем запросе подтянет объект из origin.
     * - Используем cdnDomain (CNAME) чтобы избежать миграции ссылок при смене origin.
     */
    @Override
    public String publicUrl(String key) {
        // Если задан CDN-домен — используем его (рекомендуется)
        if (StringUtils.hasText(props.getCdnDomain())) {
            String cdn = props.getCdnDomain();
            if (cdn.endsWith("/")) cdn = cdn.substring(0, cdn.length() - 1);
            return cdn + "/" + key;
        }

        // fallback: составляем path-style URL: {endpoint}/{bucket}/{key}
        String endpoint = props.getEndpoint();
        if (endpoint.endsWith("/")) endpoint = endpoint.substring(0, endpoint.length() - 1);
        return endpoint + "/" + props.getBucket() + "/" + key;

    }
}
