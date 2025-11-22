package org.vornex.listing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.vornex.listing.MapToJsonConverter;
import org.vornex.listing.Money;
import org.vornex.listing.enums.ItemCondition;
import org.vornex.listing.enums.ListingStatus;
import org.vornex.listing.enums.ModerationStatus;

import java.time.Instant;
import java.util.*;

/**
 * Сущность объявления (Listing) — production-oriented:
 * - UUID PK
 * - soft-delete (deleted = true)
 * - optimistic locking (@Version)
 * - JSON attributes через AttributeConverter
 * - минимальная валидация полей
 * <p>
 * Обратите внимание:
 * - Правило "только владелец может редактировать" реализуется не в entity,
 * а в сервисном/безопасном слое (Security + сервис). Здесь хранятся поля,
 * необходимые для принятия решения (owner, status, moderation).
 */
@Entity
@Table(name = "listings",
        indexes = {
                @Index(name = "idx_listing_status", columnList = "status"),
                @Index(name = "idx_listing_category", columnList = "category"),
                @Index(name = "idx_listing_created_at", columnList = "created_at"),
                @Index(name = "idx_listing_price", columnList = "price_amount")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Listing {

    /**
     * Идентификатор — UUID.
     * UUID хорошо подходит для модульного монолита / микросервисной архитектуры.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) //обычно генерирует до persist
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Заголовок объявления — обязательный, короткий.
     */
    @NotBlank
    @Size(max = 120)
    @Column(name = "title", nullable = false, length = 120)
    private String title;

    /**
     * Подробное описание — может быть длинным.
     */
    @Size(max = 10000)
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /**
     * Цена — BigDecimal для точности. Валюта — ISO 4217 код (например "RUB", "EUR").
     */
    @Embedded
    private Money price;

    /**
     * Категория (может быть FK к таблице категорий). Здесь просто id категории.
     * Если нужна полноценная иерархия — сделаем отдельную сущность Category.
     */
    @Column(name = "category", nullable = false)
    private String category;

    /**
     * Состояние товара (enum): новый, б/у и т.д.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false, length = 16)
    @Builder.Default
    private ItemCondition itemCondition = ItemCondition.USED;

    /**
     * Статус объявления (на уровне бизнес-логики): draft/published/archived/removed.
     * Публикация обычно проходит через workflow (например, auto-publish после модерации).
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private ListingStatus status = ListingStatus.DRAFT;

    /**
     * Статус модерации (если у тебя есть модерация контента).
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 16)
    @Builder.Default
    private ModerationStatus moderationStatus = ModerationStatus.NOT_REQUIRED;

    /**
     * Причина отклонения при модерации — nullable.
     */
    @Column(name = "moderation_rejection_reason", length = 1000)
    private String moderationRejectionReason;

    /**
     * при update/delete будем проверять из токена userId и сравнивать с этим.
     * Плюсы UUID:
     * - Компилятор/IDE помогают (ты не случайно засунул в поле что-то не UUID).
     * - Меньше преобразований при работе с DB-driver/hibernate.
     * - Индексы на uuid обычно компактнее и быстрее чем на varchar.
     * - Явная семантика — это именно UUID.
     */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId; //

    /**
     * Локация: город/регион/координаты. Можно вынести в отдельную embedded сущность,
     * но для простоты здесь — отдельные поля.
     */
    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "region", length = 255)
    private String region;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    /**
     * Дополнительные атрибуты (характеристики) — гибкая JSON структура.
     * Сохраняем Map<String, String> через конвертер в JSON.
     * Для PostgreSQL можно заменить на jsonb с помощью специализированного типа.
     */
    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "attributes", columnDefinition = "text")
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();

    /**
     * Привязка к изображениям: обычно отдельная сущность Attachment с ссылкой на S3/CDN.
     * Здесь используем OneToMany (mappedBy = "listing") — предполагается, что Attachment хранит FK.
     */
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    /**
     * Счётчики: просмотры (можно вынести в отдельную таблицу/сервис для write-heavy нагрузок).
     * Для простоты — поле long. Если ожидается много инкрементов, лучше Redis/метрики + периодическое обновление.
     */
    @Column(name = "views_count")
    @Builder.Default
    private Long viewsCount = 0L;

    /**
     * Даты: created/updated/published/expired.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Optimistic locking.
     */
    @Version
    @Column(name = "version")
    private Long version;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        // корректно работает с Hibernate прокси
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Listing other = (Listing) o;
        // Если оба id заданы — сравниваем по id
        return id != null && id.equals(other.id);
        // Если id == null => считаем объекты разными (transient != persistent)
    }

    @Override
    public int hashCode() {
        // Если id задан — используем его hashCode.
        // Если id == null — используем identityHashCode(this) (не идеально, но безопасно).
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "Listing{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=..." +
                '}';
    }

}
