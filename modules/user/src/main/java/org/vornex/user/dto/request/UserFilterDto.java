package org.vornex.user.dto.request;

import lombok.Data;
import org.vornex.userapi.AccountStatus;

@Data
public class UserFilterDto {
    private AccountStatus status;
    private String city;
    private String roleName;
}
