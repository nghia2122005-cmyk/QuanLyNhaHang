package vn.ueh.kltn.restaurant.controller;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ueh.kltn.restaurant.dto.request.ChangeRoleRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateAccountRequest;
import vn.ueh.kltn.restaurant.dto.response.AccountDetailsResponse;
import vn.ueh.kltn.restaurant.service.AccountService;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accounts;

    @GetMapping
    public List<AccountDetailsResponse> list() {
        return accounts.list();
    }

    @PostMapping
    public AccountDetailsResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return accounts.create(request);
    }

    @PatchMapping("/{id}/lock")
    @Operation(
            description =
                    "D10: Khóa chặn đăng nhập mới. JWT đã cấp vẫn giữ quyền đến hết hạn 12 giờ;"
                        + " không thu hồi token.")
    public AccountDetailsResponse lock(@PathVariable Long id) {
        return accounts.lock(id);
    }

    @PatchMapping("/{id}/unlock")
    public AccountDetailsResponse unlock(@PathVariable Long id) {
        return accounts.unlock(id);
    }

    @PatchMapping("/{id}/role")
    @Operation(
            description =
                    "D10: JWT cũ giữ role đến hết hạn 12 giờ; /me trả role token. Đăng nhập lại"
                        + " nhận role mới.")
    public AccountDetailsResponse role(
            @PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request) {
        return accounts.changeRole(id, request);
    }
}
