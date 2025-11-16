package org.vornex.listing.dto.attachmentDto;

import java.util.UUID;

public record AttachmentRequestDto(
        String fileName,
        String contentType,
        Long size,
        UUID listingId
) {
}
