package vn.ueh.kltn.restaurant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static vn.ueh.kltn.restaurant.TestFixtures.account;
import static vn.ueh.kltn.restaurant.TestFixtures.id;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.ueh.kltn.restaurant.dto.request.ChangeRoleRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateAccountRequest;
import vn.ueh.kltn.restaurant.dto.response.AccountDetailsResponse;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.enums.AccountStatus;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;

import java.util.Optional;

class AccountServiceImplTest {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final AccountServiceImpl service = new AccountServiceImpl(accounts, encoder, user);

    private CreateAccountRequest request() {
        return new CreateAccountRequest("Nhân viên", "0910000001", "Demo@123", Role.WAITER);
    }

    @Test
    void duplicatePhoneIsBlockedBeforeHashing() {
        when(accounts.existsByPhone("0910000001")).thenReturn(true);
        assertEquals(
                ErrorCode.PHONE_ALREADY_EXISTS,
                assertThrows(BusinessException.class, () -> service.create(request()))
                        .getErrorCode());
        verifyNoInteractions(encoder);
    }

    @Test
    void duplicateRaceHasSpecificCode() {
        when(encoder.encode(any())).thenReturn("hash");
        when(accounts.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));
        assertEquals(
                ErrorCode.PHONE_ALREADY_EXISTS,
                assertThrows(BusinessException.class, () -> service.create(request()))
                        .getErrorCode());
    }

    @Test
    void creatingAccountHashesPasswordAndStartsActive() {
        when(encoder.encode("Demo@123")).thenReturn("hash");
        when(accounts.saveAndFlush(any())).thenAnswer(a -> id(a.getArgument(0), 10L));
        AccountDetailsResponse result = service.create(request());
        assertEquals(AccountStatus.ACTIVE, result.status());
        verify(accounts).saveAndFlush(argThat(a -> a.getPasswordHash().equals("hash")));
    }

    @Test
    void cannotModifySelfIncludingUnlock() {
        when(user.accountId()).thenReturn(2L);
        assertEquals(
                ErrorCode.CANNOT_MODIFY_SELF,
                assertThrows(BusinessException.class, () -> service.lock(2L)).getErrorCode());
        assertThrows(BusinessException.class, () -> service.unlock(2L));
        assertThrows(
                BusinessException.class,
                () -> service.changeRole(2L, new ChangeRoleRequest(Role.OWNER)));
        verifyNoInteractions(accounts);
    }

    @Test
    void lockUnlockAndSameRoleAreIdempotent() {
        when(user.accountId()).thenReturn(1L);
        Account a = account();
        when(accounts.findById(2L)).thenReturn(Optional.of(a));
        service.lock(2L);
        assertEquals(AccountStatus.LOCKED, service.lock(2L).status());
        service.unlock(2L);
        assertEquals(AccountStatus.ACTIVE, service.unlock(2L).status());
        assertEquals(
                Role.MANAGER, service.changeRole(2L, new ChangeRoleRequest(Role.MANAGER)).role());
    }

    @Test
    void missingAccountIs404() {
        when(user.accountId()).thenReturn(1L);
        assertEquals(
                ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.lock(99L)).getErrorCode());
    }
}
