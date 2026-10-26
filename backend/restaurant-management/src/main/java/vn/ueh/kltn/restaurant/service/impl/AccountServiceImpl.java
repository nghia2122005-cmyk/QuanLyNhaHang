package vn.ueh.kltn.restaurant.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.kltn.restaurant.dto.request.ChangeRoleRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateAccountRequest;
import vn.ueh.kltn.restaurant.dto.response.AccountDetailsResponse;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.mapper.AccountMapper;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;
import vn.ueh.kltn.restaurant.service.AccountService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final CurrentUser currentUser;

    public List<AccountDetailsResponse> list() {
        return accounts.findAllByOrderByIdAsc().stream().map(AccountMapper::toDetails).toList();
    }

    @Transactional
    public AccountDetailsResponse create(CreateAccountRequest request) {
        if (accounts.existsByPhone(request.phone()))
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        var account =
                new Account(
                        request.fullName(),
                        request.phone(),
                        encoder.encode(request.password()),
                        request.role());
        try {
            return AccountMapper.toDetails(accounts.saveAndFlush(account));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
    }

    @Transactional
    public AccountDetailsResponse lock(Long id) {
        Account a = editable(id);
        a.lock();
        return AccountMapper.toDetails(a);
    }

    @Transactional
    public AccountDetailsResponse unlock(Long id) {
        Account a = editable(id);
        a.unlock();
        return AccountMapper.toDetails(a);
    }

    @Transactional
    public AccountDetailsResponse changeRole(Long id, ChangeRoleRequest request) {
        Account a = editable(id);
        a.changeRole(request.role());
        return AccountMapper.toDetails(a);
    }

    private Account editable(Long id) {
        if (id.equals(currentUser.accountId()))
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_SELF);
        return accounts.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
