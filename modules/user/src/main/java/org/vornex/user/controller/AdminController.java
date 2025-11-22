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

    @GetMapping()
    public ResponseEntity<PagedResponse<UserDto>> getAllUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @Valid UserFilterDto filterDto
    ) {
        PagedResponse<UserDto> users = adminService.getAllUsers(page, size, filterDto);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") UUID id) {
        UserDto userDto = adminService.getUserById(id);
        return ResponseEntity.ok(userDto);
    }

    @PatchMapping("/{id}/roles")
    public ResponseEntity<Void> updateUserRoles(@PathVariable("id") UUID id,
                                                @RequestBody @Valid RoleUpdateDto dto) {
        adminService.updateUserRoles(id, dto.getRoles());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("{id}/status")
    public ResponseEntity<Void> changeUserStatus(@PathVariable("id") UUID id,
                                                 @RequestBody @Valid ChangeAccountStatusDto dto){
        adminService.changeAccountStatus(id, dto.getNewStatusStr());
        return ResponseEntity.ok().build();
    }

}
