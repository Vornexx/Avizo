package org.vornex.listing.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.vornex.exception.BadRequestException;
import org.vornex.listing.dto.CreateListingDto;
import org.vornex.listing.dto.ListingResponseDto;
import org.vornex.listing.dto.UpdateListingDto;
import org.vornex.listing.service.ListingService;

import java.net.URI;
import java.util.UUID;

/**
 * REST контроллер для операций с объявлениями (Listing).
 * <p>
 * Правила:
 * - Контроллер легкий — он валидирует ввод и делегирует работу сервису.
 * - Возвращаемые статусы:
 * - POST /api/listings -> 201 Created + Location header + body с созданным DTO
 * - GET /api/listings/{id} -> 200 OK + body
 * - PATCH /api/listings/{id} -> 200 OK + body (partial update)
 * - POST /api/listings/{id}/publish -> 204 No Content
 * - DELETE /api/listings/{id} -> 204 No Content
 * - POST /api/listings/{id}/views -> 204 No Content (increment view counter)
 * <p>
 * Контроллер не выполняет авторизацию напрямую — это делает бизнес-слой (ListingService)
 * через SecurityContextUtils / @PreAuthorize (по выбору). Это упрощает unit-тестирование.
 */
@RestController
@RequestMapping(path = "/api/listings", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@RequiredArgsConstructor
@Slf4j
public class ListingController {

    private final ListingService listingService;

    // ---------------- CREATE ----------------

    /**
     * Создать объявление (черновик).
     * <p>
     * Поток:
     * - Валидация DTO (@Valid)
     * - Сервис создаёт объявление и возвращает DTO
     * - Контроллер возвращает 201 Created и ставит Location: /api/listings/{id}
     *
     * @param dto CreateListingDto — DTO для создания (title, price, category, attachmentIds и т.д.)
     * @return ResponseEntity с body ListingResponseDto и Location header
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListingResponseDto> createListing(@Valid @RequestBody CreateListingDto dto) {
        ListingResponseDto created = listingService.createListing(dto);

        // Формируем Location: /api/listings/{id}
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    // ---------------- GET ----------------

    /**
     * Получить объявление по id.
     * <p>
     * Возвращает 200 OK с DTO. Благодаря @Where на entity удалённые записи не вернутся.
     *
     * @param id UUID объявления
     */
    @GetMapping(path = "/{id}")
    public ResponseEntity<ListingResponseDto> getById(@PathVariable("id") UUID id) {
        ListingResponseDto dto = listingService.getById(id);
        return ResponseEntity.ok(dto);
    }

    // ---------------- PATCH / UPDATE ----------------

    /**
     * Частичное обновление объявления (PATCH semantics).
     * <p>
     * Правила:
     * - DTO содержит nullable поля; null = не менять.
     * - Сервис выполняет проверку прав (владелец или admin), optimistic version и пр.
     *
     * @param id  UUID объявления
     * @param dto UpdateListingDto — частичный набор полей для обновления
     * @return 200 OK с обновлённым ListingResponseDto
     */
    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListingResponseDto> updateListing(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateListingDto dto
    ) {
        // Проверка, что dto не null — @Valid уже сделает это, но дополнительная защита
        if (dto == null) throw new BadRequestException("Request body is required");

        ListingResponseDto updated = listingService.updateListing(id, dto);
        return ResponseEntity.ok(updated);
    }

    // ---------------- PUBLISH ----------------

    /**
     * Опубликовать объявление.
     * <p>
     * Поток:
     * - Сервис проверяет права и инварианты (title, price, category, attachments)
     * - Смена статуса на PUBLISHED и сохранение
     * <p>
     * Возвращает 204 No Content при успехе.
     */
    @PostMapping(path = "/{id}/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void publish(@PathVariable("id") UUID id) {
        listingService.publish(id);
    }

    // ---------------- DELETE ----------------

    /**
     * Удалить объявление (soft-delete).
     * <p>
     * Поток:
     * - Сервис проверяет права (owner/admin)
     * - Планы удаления attachments через outbox
     * - Soft-delete записи listing (через @SQLDelete)
     * <p>
     * Возвращаем 204 No Content.
     */
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id) {
        listingService.deleteListing(id);
    }

    // ---------------- INCREMENT VIEWS ----------------

    /**
     * Инкремент счётчика просмотров.
     * <p>
     * Возвращаем 204 No Content — быстрый endpoint для фронта/плеера.
     */
    @PostMapping(path = "/{id}/views")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void incrementViews(@PathVariable("id") UUID id) {
        listingService.incrementViews(id);
    }
}
