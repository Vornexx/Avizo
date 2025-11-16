package org.vornex.listing.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vornex.listing.dto.attachmentDto.AttachmentCompletedResponseDto;
import org.vornex.listing.dto.attachmentDto.AttachmentRequestDto;
import org.vornex.listing.dto.attachmentDto.AttachmentResponseDto;
import org.vornex.listing.entity.Attachment;
import org.vornex.listing.service.AttachmentService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/uploads")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/presign")
    public ResponseEntity<AttachmentResponseDto> presignUrl(@RequestBody AttachmentRequestDto attachmentDto) {
        return ResponseEntity.ok(attachmentService.presignUrl(attachmentDto));
        // возвращает временную ссылку для загрузки файла и ключ фронту
    }

    @PostMapping("/complete")
    public ResponseEntity<AttachmentCompletedResponseDto> complete(@RequestParam("key") String key) {
        return ResponseEntity.ok(attachmentService.completeUpload(key));
        //бек проверяет S3 HEAD, сохраняет Attachment в бд и возвращает id + fileUrl (preview)
    }


    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteAttachment(@RequestBody Attachment attachment) {
        attachmentService.delete(attachment);
        return ResponseEntity.ok().build();
    }


}
