package vn.ueh.kltn.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import vn.ueh.kltn.restaurant.enums.AreaStatus;

import java.util.UUID;

@Entity
@Table(name = "dining_area")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiningArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AreaStatus status;

    public DiningArea(String name) {
        this.name = name;
        this.code =
                switch (name) {
                    case "Tầng 1" -> "T01";
                    case "Tầng 2" -> "T02";
                    case "Sân vườn", "Sân Vườn" -> "SV";
                    default -> "KV" + UUID.randomUUID().toString().replace("-", "");
                };

        this.status = AreaStatus.ACTIVE;
    }

    public DiningArea(String name, String code) {
        this.name = name;
        this.code = code;
        this.status = AreaStatus.ACTIVE;
    }

    public void updateInfo(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public void deactivate() {
        status = AreaStatus.INACTIVE;
    }

    public void activate() {
        status = AreaStatus.ACTIVE;
    }
}
