package vn.ueh.kltn.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ueh.kltn.restaurant.enums.AccountStatus;
import vn.ueh.kltn.restaurant.enums.Role;

@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", nullable = false, length = 15, unique = true)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccountStatus status;

    public Account(String fullName, String phone, String passwordHash, Role role) {
        this.fullName = fullName; this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = AccountStatus.ACTIVE;
    }
    public void lock() { status = AccountStatus.LOCKED; }
    public void unlock() { status = AccountStatus.ACTIVE; }
    public void changeRole(Role role) { this.role = Objects.requireNonNull(role); }
}
