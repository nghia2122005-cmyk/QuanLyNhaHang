package vn.ueh.kltn.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.ueh.kltn.restaurant.entity.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByPhone(String phone);

    boolean existsByPhone(String phone);

    List<Account> findAllByOrderByIdAsc();
}
