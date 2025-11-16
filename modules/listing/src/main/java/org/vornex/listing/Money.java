package org.vornex.listing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Embeddable
@Getter
public final class Money {
    @NotNull
    @PositiveOrZero
    @Column(name = "price_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;


    @NotBlank
    @Size(max = 3)
    @Column(name = "price_currency", length = 3, nullable = false)
    private String currency;

    // нужен конструктор для Jackson
    @JsonCreator
    public Money(@JsonProperty("amount") BigDecimal amount,
                 @JsonProperty("currency") String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public Money() {

    }
}