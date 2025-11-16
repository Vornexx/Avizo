package org.vornex.listing.dto.attachmentDto;

public record AttachmentDto(
        Long id,
        String storageKey,
        String publicUrl
) {
}
