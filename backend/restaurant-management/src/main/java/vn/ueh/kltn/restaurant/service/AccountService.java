package vn.ueh.kltn.restaurant.service;

import vn.ueh.kltn.restaurant.dto.request.ChangeRoleRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateAccountRequest;
import vn.ueh.kltn.restaurant.dto.response.AccountDetailsResponse;

import java.util.List;

public interface AccountService {
    List<AccountDetailsResponse> list();

    AccountDetailsResponse create(CreateAccountRequest request);

    AccountDetailsResponse lock(Long id);

    AccountDetailsResponse unlock(Long id);

    AccountDetailsResponse changeRole(Long id, ChangeRoleRequest request);
}
