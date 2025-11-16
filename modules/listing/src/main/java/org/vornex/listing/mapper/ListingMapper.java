package org.vornex.listing.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.vornex.listing.dto.ListingResponseDto;
import org.vornex.listing.dto.attachmentDto.AttachmentDto;
import org.vornex.listing.entity.Listing;
import org.vornex.listing.service.StorageService;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ListingMapper {

    @Autowired //иначе не скомпилиться тк нужен конструктор без параметров (через @Context неудобно будет каждый раз storage прокидывать)
    protected  StorageService storageService;

    @Mapping(target = "attachments", expression = "java(mapAttachments(listing))")
    public abstract ListingResponseDto toDto(Listing listing);

    protected List<AttachmentDto> mapAttachments(Listing listing) {
        if (listing.getAttachments() == null) return Collections.emptyList();
        return listing.getAttachments().stream()
                .map(att -> new AttachmentDto(
                        att.getId(),
                        att.getStorageKey(),
                        storageService.publicUrl(att.getStorageKey())
                ))
                .toList();
    }
}
