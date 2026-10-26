package vn.ueh.kltn.restaurant.service;

import vn.ueh.kltn.restaurant.dto.request.LoginRequest;
import vn.ueh.kltn.restaurant.dto.response.AccountResponse;
import vn.ueh.kltn.restaurant.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    AccountResponse me();
}
