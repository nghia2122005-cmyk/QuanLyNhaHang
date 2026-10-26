package vn.ueh.kltn.restaurant.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ueh.kltn.restaurant.dto.request.LoginRequest;
import vn.ueh.kltn.restaurant.dto.response.AccountResponse;
import vn.ueh.kltn.restaurant.dto.response.LoginResponse;
import vn.ueh.kltn.restaurant.enums.AccountStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;
import vn.ueh.kltn.restaurant.security.JwtService;
import vn.ueh.kltn.restaurant.service.AuthService;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;
    private final String dummyHash;

    public AuthServiceImpl(AccountRepository accounts, PasswordEncoder passwordEncoder, JwtService jwtService, CurrentUser currentUser) {
        this.accounts = accounts; this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService; this.currentUser = currentUser;
        this.dummyHash = passwordEncoder.encode("dummy-login-verification");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        var account = accounts.findByPhone(request.phone());
        // Always run BCrypt, including for an unknown phone number.
        var hash = account.map(value -> value.getPasswordHash()).orElse(dummyHash);
        if (!passwordEncoder.matches(request.password(), hash) || account.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (account.get().getStatus() == AccountStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        return jwtService.issue(account.get());
    }

    @Override
    public AccountResponse me() {
        // D10: an issued JWT keeps its role until expiry or a fresh login.
        return accounts.findById(currentUser.accountId()).map(account ->
                        new AccountResponse(account.getId(), account.getFullName(), currentUser.role()))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
