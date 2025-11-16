package org.vornex.listing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.vornex.listing.Money;
import org.vornex.listing.enums.ItemCondition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


/**
 * DTO для создания объявления — валидируем входные данные на уровне контроллера.
 * Поля минимальны, не передаем system-managed поля (id, ownerId, createdAt и т.д.).
 */
// DTO для создания
public record CreateListingDto(
        String title,
        String description,
        Money price,
        String category,
        ItemCondition itemCondition,
        Map<String, String> attributes,
        List<Long> attachmentIds // предварительно загруженные attachment ids, опционально
) {}