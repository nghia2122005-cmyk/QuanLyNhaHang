package vn.ueh.kltn.restaurant.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.ueh.kltn.restaurant.dto.request.CreateIngredientRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateStockDisposalRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateStockReceiptRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateStocktakeRequest;
import vn.ueh.kltn.restaurant.dto.request.UpdateIngredientRequest;
import vn.ueh.kltn.restaurant.dto.response.IngredientLotAttentionResponse;
import vn.ueh.kltn.restaurant.dto.response.IngredientLotResponse;
import vn.ueh.kltn.restaurant.dto.response.IngredientResponse;
import vn.ueh.kltn.restaurant.dto.response.PageResponse;
import vn.ueh.kltn.restaurant.dto.response.StockMovementResponse;
import vn.ueh.kltn.restaurant.dto.response.StockReceiptResponse;
import vn.ueh.kltn.restaurant.dto.response.StocktakeResponse;
import vn.ueh.kltn.restaurant.dto.response.StocktakeTemplateItemResponse;
import vn.ueh.kltn.restaurant.enums.IngredientStatus;
import vn.ueh.kltn.restaurant.enums.MovementType;
import vn.ueh.kltn.restaurant.service.InventoryService;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/ingredients")
    @PreAuthorize("hasAnyRole('OWNER','HEAD_CHEF')")
    public List<IngredientResponse> ingredients(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) IngredientStatus status,
            @RequestParam(required = false) Boolean belowMinStockOnly) {
        return inventoryService.listIngredients(q, status, belowMinStockOnly);
    }

    @PostMapping("/ingredients")
    @PreAuthorize("hasRole('OWNER')")
    public IngredientResponse createIngredient(
            @Valid @RequestBody CreateIngredientRequest request) {
        return inventoryService.createIngredient(request);
    }

    @PutMapping("/ingredients/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public IngredientResponse updateIngredient(
            @PathVariable Long id, @Valid @RequestBody UpdateIngredientRequest request) {
        return inventoryService.updateIngredient(id, request);
    }

    @PatchMapping("/ingredients/{id}/deactivate")
    @PreAuthorize("hasRole('OWNER')")
    public IngredientResponse deactivateIngredient(@PathVariable Long id) {
        return inventoryService.deactivateIngredient(id);
    }

    @PatchMapping("/ingredients/{id}/activate")
    @PreAuthorize("hasRole('OWNER')")
    public IngredientResponse activateIngredient(@PathVariable Long id) {
        return inventoryService.activateIngredient(id);
    }

    @GetMapping("/lots/attention")
    @PreAuthorize("hasAnyRole('OWNER','HEAD_CHEF')")
    public List<IngredientLotAttentionResponse> attentionLots(
            @RequestParam(required = false) @Min(0) @Max(90) Integer days) {
        return inventoryService.listAttentionLots(days);
    }

    @GetMapping("/ingredients/{ingredientId}/lots")
    @PreAuthorize("hasAnyRole('OWNER','HEAD_CHEF')")
    public List<IngredientLotResponse> ingredientLots(
            @PathVariable Long ingredientId,
            @RequestParam(required = false) @Min(0) @Max(90) Integer days) {
        return inventoryService.listIngredientLots(ingredientId, days);
    }

    @PostMapping("/receipts")
    @PreAuthorize("hasRole('HEAD_CHEF')")
    public StockReceiptResponse createReceipt(
            @Valid @RequestBody CreateStockReceiptRequest request) {
        return inventoryService.createReceipt(request);
    }

    @GetMapping("/receipts")
    @PreAuthorize("hasAnyRole('OWNER','HEAD_CHEF')")
    public PageResponse<StockReceiptResponse> receipts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return inventoryService.listReceipts(from, to, page, size);
    }

    @GetMapping("/stocktakes/template")
    @PreAuthorize("hasRole('HEAD_CHEF')")
    public List<StocktakeTemplateItemResponse> stocktakeTemplate() {
        return inventoryService.getStocktakeTemplate();
    }

    @PostMapping("/stocktakes")
    @PreAuthorize("hasRole('HEAD_CHEF')")
    public StocktakeResponse createStocktake(@Valid @RequestBody CreateStocktakeRequest request) {
        return inventoryService.createStocktake(request);
    }

    @GetMapping("/stocktakes")
    @PreAuthorize("hasAnyRole('OWNER','HEAD_CHEF')")
    public PageResponse<StocktakeResponse> stocktakes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return inventoryService.listStocktakes(from, to, page, size);
    }

    @PostMapping("/disposals")
    @PreAuthorize("hasRole('HEAD_CHEF')")
    public StockMovementResponse createDisposal(
            @Valid @RequestBody CreateStockDisposalRequest request) {
        return inventoryService.createDisposal(request);
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('OWNER','HEAD_CHEF')")
    public PageResponse<StockMovementResponse> movements(
            @RequestParam(required = false) Long ingredientId,
            @RequestParam(required = false) MovementType movementType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return inventoryService.listMovements(ingredientId, movementType, from, to, page, size);
    }
}
