package vn.ueh.kltn.restaurant.entity;

import java.math.BigDecimal;

/** Immutable base-unit quantities captured when cooking starts; not a domain table. */
public record IngredientRequirement(Long ingredientId, String unit, BigDecimal quantity) {}
