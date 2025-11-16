package org.vornex.listing.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    private String endpoint;        // e.g. https://storage.yandexcloud.net
    private String region = "ru-central1";
    private String accessKey;
    private String secretKey;
    private String bucket;          // имя бакета
    private String cdnDomain;
    private boolean pathStyleAccess = true; // Yandex обычно требует path-style
    private Duration presignTtl = Duration.ofMinutes(15);
}
