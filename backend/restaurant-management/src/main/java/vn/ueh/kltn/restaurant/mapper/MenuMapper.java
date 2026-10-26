package vn.ueh.kltn.restaurant.mapper;

import vn.ueh.kltn.restaurant.dto.response.MenuItemResponse;
import vn.ueh.kltn.restaurant.dto.response.RecipeLineResponse;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.entity.MenuItem;
import vn.ueh.kltn.restaurant.entity.RecipeItem;

public final class MenuMapper {
    private MenuMapper() {}

    public static MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(), item.getName(), item.getPrice(), item.getStatus());
    }

    public static RecipeLineResponse toRecipeLine(RecipeItem item) {
        Ingredient ingredient = item.getIngredient();
        return new RecipeLineResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getUnit(),
                item.getQuantityPerServing());
    }
}
