package vn.ueh.kltn.restaurant.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.kltn.restaurant.dto.response.IngredientAvailabilityResponse;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.entity.IngredientRequirement;
import vn.ueh.kltn.restaurant.entity.OrderItem;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.repository.IngredientRepository;
import vn.ueh.kltn.restaurant.repository.OrderItemRepository;
import vn.ueh.kltn.restaurant.repository.StockMovementRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared inventory locking/calculation inside the caller's workflow transaction. */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class CookingInventory {
    private final EntityManager entityManager;
    private final IngredientRepository ingredients;
    private final OrderItemRepository items;
    private final StockMovementRepository movements;

    public Map<Long, Ingredient> lock(List<Long> ids) {
        Map<Long, Ingredient> locked = new LinkedHashMap<>();
        for (Long id : ids.stream().distinct().sorted().toList()) {
            locked.put(
                    id,
                    ingredients
                            .findLockedById(id)
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ErrorCode.NOT_FOUND,
                                                    "Không tìm thấy nguyên vật liệu")));
            // Recipe reads may already have populated the persistence context before lock
            // acquisition.
            entityManager.refresh(locked.get(id), LockModeType.PESSIMISTIC_WRITE);
        }
        return locked;
    }

    public List<IngredientAvailabilityResponse> inspect(
            OrderItem item,
            List<IngredientRequirement> requirements,
            Map<Long, Ingredient> locked) {
        List<IngredientAvailabilityResponse> result = new ArrayList<>();
        for (IngredientRequirement requirement : requirements) {
            Ingredient ingredient = locked.get(requirement.ingredientId());
            if (!ingredient.getUnit().equalsIgnoreCase(requirement.unit())) {
                throw new BusinessException(
                        ErrorCode.UNIT_CHANGE_BLOCKED,
                        "Đơn vị nguyên vật liệu không khớp snapshot chế biến");
            }
            BigDecimal stock = movements.sumQuantityByIngredientId(ingredient.getId());
            BigDecimal reserved = items.reservedQuantity(ingredient.getId(), item.getId());
            BigDecimal available = stock.subtract(reserved);
            BigDecimal projected = available.subtract(requirement.quantity());
            result.add(
                    new IngredientAvailabilityResponse(
                            ingredient.getId(),
                            ingredient.getName(),
                            ingredient.getUnit(),
                            stock,
                            reserved,
                            available,
                            requirement.quantity(),
                            projected,
                            ingredient.getMinStock(),
                            projected.negate().max(BigDecimal.ZERO)));
        }
        return List.copyOf(result);
    }
}
