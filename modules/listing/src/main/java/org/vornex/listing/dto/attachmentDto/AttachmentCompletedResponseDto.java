package org.vornex.listing.dto.attachmentDto;

public record AttachmentCompletedResponseDto(
        Long attachmentId,
        String fileUrl //public url
) {
}
