package org.vornex.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeAccountStatusDto {

    @NotNull(message = "Новый статус не может быть пустым")
    private String newStatusStr;
}
