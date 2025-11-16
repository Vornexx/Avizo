package org.vornex.listing.service;

import org.vornex.listing.dto.attachmentDto.AttachmentCompletedResponseDto;
import org.vornex.listing.dto.attachmentDto.AttachmentRequestDto;
import org.vornex.listing.dto.attachmentDto.AttachmentResponseDto;
import org.vornex.listing.entity.Attachment;

public interface AttachmentService {
    AttachmentResponseDto presignUrl(AttachmentRequestDto attachmentDto);

    AttachmentCompletedResponseDto completeUpload(String key);

    void delete(Attachment attachment);
}
