package org.vornex.listing.dto.attachmentDto;

import java.time.Instant;

public record AttachmentResponseDto(
        String uploadUrl,
        String key,
        Instant expiresInSeconds
) {
}
