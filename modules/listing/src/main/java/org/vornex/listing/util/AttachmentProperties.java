package org.vornex.listing.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Getter
@Setter // обязательно для hot reload
@ConfigurationProperties(prefix = "app.attachments")
public class AttachmentProperties {
    private Duration presignTtl = Duration.ofMinutes(15); //без final т.к. это мешает yaml биндить сюда значения.
    private long maxFileSize = 10 * 1024 * 1024L;
    private int OutboxMaxAttempts = 50;
    private String[] allowedContentTypes = new String[]{
            "image/jpeg", "image/png", "image/webp", "image/gif"
    };

}
