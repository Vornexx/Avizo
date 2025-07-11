package org.vornex.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.vornex.user.dto.internal.UserDto;
import org.vornex.user.dto.request.ChangeAccountStatusDto;
import org.vornex.user.dto.request.RoleUpdateDto;
import org.vornex.user.dto.request.UserFilterDto;
import org.vornex.user.dto.response.PagedResponse;
import org.vornex.user.service.AdminService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminController {
    private final AdminService adminService;

    // возможно стоит возвращать не полное dto, а специальный какой-нибудь.
    @GetMapping()
    public ResponseEntity<PagedResponse<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Valid UserFilterDto filterDto
    ) {
        PagedResponse<UserDto> users = adminService.getAllUsers(page, size, filterDto);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        UserDto userDto = adminService.getUserById(id);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<Void> updateUserRoles(@PathVariable UUID id,
                                                @RequestBody @Valid RoleUpdateDto dto) {
        adminService.updateUserRoles(id, dto.getRoles());
        return ResponseEntity.ok().build();
    }

    @PutMapping("{id}/status")
    public ResponseEntity<Void> changeUserStatus(@PathVariable UUID id,
                                                 @RequestBody @Valid ChangeAccountStatusDto dto){
        adminService.changeAccountStatus(id, dto.getNewStatus());
        return ResponseEntity.ok().build();
    }

}
