package vn.ueh.kltn.restaurant.mapper;

import vn.ueh.kltn.restaurant.dto.response.IngredientLotAttentionResponse;
import vn.ueh.kltn.restaurant.dto.response.IngredientLotResponse;
import vn.ueh.kltn.restaurant.dto.response.IngredientResponse;
import vn.ueh.kltn.restaurant.dto.response.StockMovementResponse;
import vn.ueh.kltn.restaurant.dto.response.StockReceiptItemResponse;
import vn.ueh.kltn.restaurant.dto.response.StockReceiptResponse;
import vn.ueh.kltn.restaurant.dto.response.StocktakeItemResponse;
import vn.ueh.kltn.restaurant.dto.response.StocktakeResponse;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.entity.FoodOrder;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.entity.IngredientLot;
import vn.ueh.kltn.restaurant.entity.ServiceSession;
import vn.ueh.kltn.restaurant.entity.StockMovement;
import vn.ueh.kltn.restaurant.entity.StockReceipt;
import vn.ueh.kltn.restaurant.entity.StockReceiptItem;
import vn.ueh.kltn.restaurant.entity.Stocktake;
import vn.ueh.kltn.restaurant.entity.StocktakeItem;
import vn.ueh.kltn.restaurant.enums.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class InventoryMapper {

    private InventoryMapper() {}

    public static IngredientResponse toIngredientResponse(
            Ingredient ingredient, BigDecimal currentStock) {
        return toIngredientResponse(ingredient, currentStock, BigDecimal.ZERO);
    }

    public static IngredientResponse toIngredientResponse(
            Ingredient ingredient, BigDecimal currentStock, BigDecimal reserved) {
        return toIngredientResponse(ingredient, currentStock, reserved, null, 0);
    }

    public static IngredientResponse toIngredientResponse(
            Ingredient ingredient,
            BigDecimal currentStock,
            BigDecimal reserved,
            LocalDate nearestExpiryDate,
            long lotCount) {
        BigDecimal stock = currentStock != null ? currentStock : BigDecimal.ZERO;
        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getUnit(),
                ingredient.getMinStock(),
                ingredient.isTrackLot(),
                ingredient.isTrackExpiry(),
                ingredient.getStatus(),
                stock,
                ingredient.isBelowMinStock(stock.subtract(reserved)),
                reserved,
                stock.subtract(reserved),
                nearestExpiryDate,
                lotCount);
    }

    public static IngredientLotResponse toLotResponse(
            IngredientLot lot, BigDecimal initialQuantity, LocalDate today, int days) {
        String status =
                lot.getExpiryDate() == null
                        ? "NO_EXPIRY"
                        : lot.isExpired(today)
                                ? "EXPIRED"
                                : lot.isExpiringSoon(today, days) ? "EXPIRING_SOON" : "VALID";
        Long daysRemaining =
                lot.getExpiryDate() == null
                        ? null
                        : ChronoUnit.DAYS.between(today, lot.getExpiryDate());
        return new IngredientLotResponse(
                lot.getId(),
                lot.getIngredient().getId(),
                lot.getIngredient().getName(),
                lot.getIngredient().getUnit(),
                lot.getLotNumber(),
                lot.getExpiryDate(),
                initialQuantity != null ? initialQuantity : BigDecimal.ZERO,
                status,
                daysRemaining);
    }

    public static IngredientLotAttentionResponse toLotAttentionResponse(
            IngredientLot lot, BigDecimal initialQuantity, LocalDate today) {
        String status;
        if (lot.isExpired(today)) {
            status = "EXPIRED";
        } else {
            status = "EXPIRING_SOON";
        }
        long daysRemaining =
                lot.getExpiryDate() != null
                        ? ChronoUnit.DAYS.between(today, lot.getExpiryDate())
                        : 0;

        return new IngredientLotAttentionResponse(
                lot.getId(),
                lot.getIngredient().getId(),
                lot.getIngredient().getName(),
                lot.getIngredient().getUnit(),
                lot.getLotNumber(),
                lot.getExpiryDate(),
                initialQuantity != null ? initialQuantity : BigDecimal.ZERO,
                status,
                daysRemaining);
    }

    public static StockReceiptResponse toReceiptResponse(StockReceipt receipt) {
        List<StockReceiptItemResponse> items = new ArrayList<>();
        if (receipt.getItems() != null) {
            for (StockReceiptItem item : receipt.getItems()) {
                IngredientLot lot = item.getLot();
                items.add(
                        new StockReceiptItemResponse(
                                item.getId(),
                                item.getIngredient().getId(),
                                item.getIngredient().getName(),
                                item.getIngredient().getUnit(),
                                lot != null ? lot.getId() : null,
                                lot != null ? lot.getLotNumber() : null,
                                lot != null ? lot.getExpiryDate() : null,
                                item.getQuantity()));
            }
        }
        return new StockReceiptResponse(
                receipt.getId(),
                receipt.getCreatedBy().getId(),
                receipt.getCreatedBy().getFullName(),
                receipt.getCreatedAt(),
                items);
    }

    public static StocktakeResponse toStocktakeResponse(Stocktake stocktake) {
        List<StocktakeItemResponse> items = new ArrayList<>();
        if (stocktake.getItems() != null) {
            for (StocktakeItem item : stocktake.getItems()) {
                items.add(
                        new StocktakeItemResponse(
                                item.getId(),
                                item.getIngredient().getId(),
                                item.getIngredient().getName(),
                                item.getIngredient().getUnit(),
                                item.getSystemQuantity(),
                                item.getActualQuantity(),
                                item.calculateDifference(),
                                item.getNote()));
            }
        }
        return new StocktakeResponse(
                stocktake.getId(),
                stocktake.getCreatedBy().getId(),
                stocktake.getCreatedBy().getFullName(),
                stocktake.getCreatedAt(),
                items);
    }

    public static StockMovementResponse toMovementResponse(StockMovement sm) {
        String source;
        if (sm.getMovementType() == MovementType.RECEIPT) {
            source =
                    sm.getReceiptItem() != null
                            ? "Phiếu nhập #" + sm.getReceiptItem().getReceipt().getId()
                            : "Phiếu nhập";
        } else if (sm.getMovementType() == MovementType.CONSUMPTION) {
            if (sm.getOrderItem() != null) {
                FoodOrder order = sm.getOrderItem().getOrder();
                ServiceSession session = order != null ? order.getSession() : null;
                DiningTable table = session != null ? session.getTable() : null;
                source =
                        (table != null ? "Bàn " + table.getName() + " - " : "")
                                + sm.getOrderItem().getMenuItem().getName()
                                + (session != null ? " (Phiên #" + session.getId() + ")" : "");
            } else {
                source = "Tiêu hao chế biến";
            }
        } else if (sm.getMovementType() == MovementType.STOCKTAKE_ADJUSTMENT) {
            source =
                    sm.getStocktakeItem() != null
                            ? "Kiểm kê #" + sm.getStocktakeItem().getStocktake().getId()
                            : "Kiểm kê";
        } else if (sm.getMovementType() == MovementType.DISPOSAL) {
            source = "Xuất hủy";
        } else {
            source = sm.getMovementType().name();
        }

        IngredientLot lot = sm.getLot();
        return new StockMovementResponse(
                sm.getId(),
                sm.getIngredient().getId(),
                sm.getIngredient().getName(),
                sm.getIngredient().getUnit(),
                lot != null ? lot.getId() : null,
                lot != null ? lot.getLotNumber() : null,
                lot != null ? lot.getExpiryDate() : null,
                sm.getMovementType(),
                sm.getQuantityChange(),
                sm.getReason(),
                sm.getPerformedBy().getId(),
                sm.getPerformedBy().getFullName(),
                sm.getOccurredAt(),
                source);
    }
}
