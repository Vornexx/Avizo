package org.vornex.listing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.vornex.listing.Money;
import org.vornex.listing.enums.ItemCondition;
import org.vornex.listing.enums.ListingStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// DTO для патч-обновления (все поля nullable -> patch semantics)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateListingDto {
    private String title;
    private String description;
    private Money price;
    private String category;
    private ItemCondition itemCondition;
    private Map<String, String> attributes;
    private List<Long> attachmentIds;
    private Long version;
}
