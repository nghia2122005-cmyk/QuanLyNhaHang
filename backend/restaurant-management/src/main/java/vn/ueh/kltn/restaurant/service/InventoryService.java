package vn.ueh.kltn.restaurant.service;

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
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.enums.IngredientStatus;
import vn.ueh.kltn.restaurant.enums.MovementType;

import java.time.Instant;
import java.util.List;

public interface InventoryService {

    List<IngredientResponse> listIngredients(
            String q, IngredientStatus status, Boolean belowMinStockOnly);

    IngredientResponse createIngredient(CreateIngredientRequest request);

    IngredientResponse updateIngredient(Long id, UpdateIngredientRequest request);

    IngredientResponse deactivateIngredient(Long id);

    IngredientResponse activateIngredient(Long id);

    List<IngredientLotAttentionResponse> listAttentionLots(Integer days);

    List<IngredientLotResponse> listIngredientLots(Long ingredientId, Integer days);

    StockReceiptResponse createReceipt(CreateStockReceiptRequest request);

    StockReceiptResponse createReceiptForSeed(CreateStockReceiptRequest request, Account actor);

    PageResponse<StockReceiptResponse> listReceipts(Instant from, Instant to, int page, int size);

    List<StocktakeTemplateItemResponse> getStocktakeTemplate();

    StocktakeResponse createStocktake(CreateStocktakeRequest request);

    StocktakeResponse createStocktakeForSeed(CreateStocktakeRequest request, Account actor);

    PageResponse<StocktakeResponse> listStocktakes(Instant from, Instant to, int page, int size);

    StockMovementResponse createDisposal(CreateStockDisposalRequest request);

    PageResponse<StockMovementResponse> listMovements(
            Long ingredientId,
            MovementType movementType,
            Instant from,
            Instant to,
            int page,
            int size);
}
