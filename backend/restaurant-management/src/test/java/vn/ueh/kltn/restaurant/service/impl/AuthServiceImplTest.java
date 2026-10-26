package vn.ueh.kltn.restaurant.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.ueh.kltn.restaurant.dto.request.LoginRequest;
import vn.ueh.kltn.restaurant.dto.response.AccountResponse;
import vn.ueh.kltn.restaurant.dto.response.LoginResponse;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;
import vn.ueh.kltn.restaurant.security.JwtService;

class AuthServiceImplTest {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private AuthServiceImpl service;
    private Account account;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(accounts, encoder, jwtService, currentUser);
        account = new Account("Nhân viên phục vụ", "0900000004", encoder.encode("Demo@123"), Role.WAITER);
    }

    @Test
    void validCredentialsIssueToken() {
        when(accounts.findByPhone("0900000004")).thenReturn(Optional.of(account));
        var expected = new LoginResponse("signed-token", Instant.now().plusSeconds(43200),
                new AccountResponse(4L, account.getFullName(), Role.WAITER));
        when(jwtService.issue(account)).thenReturn(expected);
        assertSame(expected, service.login(new LoginRequest("0900000004", "Demo@123")));
    }

    @Test
    void unknownPhoneAndWrongPasswordHaveTheSameError() {
        when(accounts.findByPhone("0900000099")).thenReturn(Optional.empty());
        when(accounts.findByPhone("0900000004")).thenReturn(Optional.of(account));
        var missing = assertThrows(BusinessException.class, () -> service.login(new LoginRequest("0900000099", "Demo@123")));
        var wrong = assertThrows(BusinessException.class, () -> service.login(new LoginRequest("0900000004", "Wrong@123")));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, missing.getErrorCode());
        assertEquals(missing.getErrorCode(), wrong.getErrorCode());
        assertEquals(missing.getMessage(), wrong.getMessage());
        verifyNoInteractions(jwtService);
    }

    @Test
    void lockedAccountWithCorrectPasswordIsRejected() {
        account.lock();
        when(accounts.findByPhone("0900000004")).thenReturn(Optional.of(account));
        var failure = assertThrows(BusinessException.class, () -> service.login(new LoginRequest("0900000004", "Demo@123")));
        assertEquals(ErrorCode.ACCOUNT_LOCKED, failure.getErrorCode());
        assertEquals("Tài khoản đã bị khóa", failure.getMessage());
        verifyNoInteractions(jwtService);
    }

    @Test
    void wrongPasswordDoesNotDiscloseLockedStatus() {
        account.lock();
        when(accounts.findByPhone("0900000004")).thenReturn(Optional.of(account));
        var failure = assertThrows(BusinessException.class, () -> service.login(new LoginRequest("0900000004", "Wrong@123")));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, failure.getErrorCode());
        verifyNoInteractions(jwtService);
    }

    @Test
    void meReadsIdentityFromCurrentUser() {
        when(currentUser.accountId()).thenReturn(4L);
        when(currentUser.role()).thenReturn(Role.WAITER);
        when(accounts.findById(4L)).thenReturn(Optional.of(account));
        assertEquals(Role.WAITER, service.me().role());
        verify(currentUser).accountId();
        verify(accounts).findById(4L);
    }

    @Test
    void meKeepsTokenRoleAfterRoleChangeAndAccountLock() {
        account.changeRole(Role.MANAGER);
        account.lock();
        when(currentUser.accountId()).thenReturn(4L);
        when(currentUser.role()).thenReturn(Role.WAITER);
        when(accounts.findById(4L)).thenReturn(Optional.of(account));
        assertEquals(Role.WAITER, service.me().role());
    }

    @Test
    void deletedIdentityIsUnauthorized() {
        when(currentUser.accountId()).thenReturn(4L);
        when(accounts.findById(4L)).thenReturn(Optional.empty());
        var failure = assertThrows(BusinessException.class, service::me);
        assertEquals(ErrorCode.UNAUTHORIZED, failure.getErrorCode());
    }
}
