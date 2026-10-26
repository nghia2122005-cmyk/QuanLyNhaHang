package vn.ueh.kltn.restaurant.service;

import vn.ueh.kltn.restaurant.dto.request.SaveMenuItemRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveRecipeRequest;
import vn.ueh.kltn.restaurant.dto.response.MenuItemResponse;
import vn.ueh.kltn.restaurant.dto.response.RecipeLineResponse;
import vn.ueh.kltn.restaurant.enums.MenuItemStatus;

import java.util.List;

public interface MenuService {
    List<MenuItemResponse> list(MenuItemStatus status, String q);

    MenuItemResponse create(SaveMenuItemRequest request);

    MenuItemResponse update(Long id, SaveMenuItemRequest request);

    MenuItemResponse discontinue(Long id);

    MenuItemResponse reactivate(Long id);

    List<RecipeLineResponse> recipe(Long id);

    List<RecipeLineResponse> saveRecipe(Long id, SaveRecipeRequest request);
}
