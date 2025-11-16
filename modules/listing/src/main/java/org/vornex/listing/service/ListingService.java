package org.vornex.listing.service;

import org.springframework.data.domain.Page;
import org.vornex.listing.dto.CreateListingDto;
import org.vornex.listing.dto.ListingDto;
import org.vornex.listing.dto.ListingResponseDto;
import org.vornex.listing.dto.UpdateListingDto;

import java.util.UUID;

public interface ListingService {
    ListingResponseDto createListing(CreateListingDto dto);
    ListingResponseDto getById(UUID id);
    ListingResponseDto updateListing(UUID id, UpdateListingDto dto);
    void publish(UUID id);
    void deleteListing(UUID id);
    void incrementViews(UUID id);
}
