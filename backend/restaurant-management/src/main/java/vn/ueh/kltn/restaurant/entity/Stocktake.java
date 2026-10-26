package vn.ueh.kltn.restaurant.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "stocktake")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stocktake {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private Account createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "stocktake", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<StocktakeItem> items = new ArrayList<>();

    public Stocktake(Account createdBy, Instant createdAt) {
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public List<StocktakeItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(StocktakeItem item) {
        item.attachTo(this);
        items.add(item);
    }
}
