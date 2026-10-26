package vn.ueh.kltn.restaurant.mapper;

import vn.ueh.kltn.restaurant.dto.response.InvoiceResponse;
import vn.ueh.kltn.restaurant.dto.response.OpenSessionPaymentResponse;
import vn.ueh.kltn.restaurant.dto.response.PaymentPreviewResponse;
import vn.ueh.kltn.restaurant.entity.DiningArea;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.entity.FoodOrder;
import vn.ueh.kltn.restaurant.entity.Invoice;
import vn.ueh.kltn.restaurant.entity.OrderItem;
import vn.ueh.kltn.restaurant.entity.ServiceSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class PaymentMapper {

    private PaymentMapper() {}

    public static InvoiceResponse toInvoiceResponse(Invoice invoice) {
        ServiceSession session = invoice.getSession();
        DiningTable table = session.getTable();
        DiningArea area = table.getArea();
        return new InvoiceResponse(
                invoice.getId(),
                session.getId(),
                table.getName(),
                area != null ? area.getName() : null,
                invoice.getCashier().getId(),
                invoice.getCashier().getFullName(),
                invoice.getPaymentMethod(),
                invoice.getAmount(),
                invoice.getPaidAt());
    }

    public static OpenSessionPaymentResponse toOpenSessionResponse(
            ServiceSession session,
            BigDecimal provisionalTotal,
            boolean hasBlockingItems,
            int totalItems) {
        DiningTable table = session.getTable();
        DiningArea area = table.getArea();
        return new OpenSessionPaymentResponse(
                session.getId(),
                table.getId(),
                table.getName(),
                area != null ? area.getName() : null,
                session.getOpenedAt(),
                provisionalTotal,
                hasBlockingItems,
                totalItems,
                table.getCode());
    }

    public static PaymentPreviewResponse toPreviewResponse(
            ServiceSession session,
            List<FoodOrder> orders,
            BigDecimal total,
            boolean hasBlocking,
            List<PaymentPreviewResponse.BlockingItemResponse> blockingItems) {
        DiningTable table = session.getTable();
        DiningArea area = table.getArea();

        List<PaymentPreviewResponse.OrderPreviewResponse> orderResponses = new ArrayList<>();
        for (FoodOrder order : orders) {
            List<PaymentPreviewResponse.OrderItemPreviewResponse> itemResponses = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                itemResponses.add(
                        new PaymentPreviewResponse.OrderItemPreviewResponse(
                                item.getId(),
                                item.getMenuItem().getId(),
                                item.getMenuItem().getName(),
                                item.getQuantity(),
                                item.getUnitPriceAtOrder(),
                                item.calculateSubtotal(),
                                item.getStatus(),
                                item.getNote(),
                                item.getUnavailableReason()));
            }
            orderResponses.add(
                    new PaymentPreviewResponse.OrderPreviewResponse(
                            order.getId(),
                            order.getCreatedAt(),
                            order.getCreatedBy().getFullName(),
                            itemResponses));
        }

        return new PaymentPreviewResponse(
                session.getId(),
                table.getId(),
                table.getName(),
                area != null ? area.getName() : null,
                session.getOpenedAt(),
                total,
                hasBlocking,
                blockingItems,
                orderResponses,
                table.getCode());
    }
}
