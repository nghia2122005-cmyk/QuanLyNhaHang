package vn.ueh.kltn.restaurant.service.impl;

import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.kltn.restaurant.dto.request.RecipeLineRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveMenuItemRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveRecipeRequest;
import vn.ueh.kltn.restaurant.dto.response.MenuItemResponse;
import vn.ueh.kltn.restaurant.dto.response.RecipeLineResponse;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.entity.MenuItem;
import vn.ueh.kltn.restaurant.entity.RecipeItem;
import vn.ueh.kltn.restaurant.enums.IngredientStatus;
import vn.ueh.kltn.restaurant.enums.MenuItemStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.mapper.MenuMapper;
import vn.ueh.kltn.restaurant.repository.IngredientRepository;
import vn.ueh.kltn.restaurant.repository.MenuItemRepository;
import vn.ueh.kltn.restaurant.repository.RecipeItemRepository;
import vn.ueh.kltn.restaurant.service.MenuService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuServiceImpl implements MenuService {
    private final MenuItemRepository menuItems;
    private final RecipeItemRepository recipes;
    private final IngredientRepository ingredients;

    public List<MenuItemResponse> list(MenuItemStatus status, String q) {
        Specification<MenuItem> filter =
                (root, query, cb) -> {
                    var conditions = new ArrayList<Predicate>();
                    if (status != null) conditions.add(cb.equal(root.get("status"), status));
                    if (q != null && !q.isBlank())
                        conditions.add(
                                cb.like(
                                        cb.lower(root.get("name")),
                                        "%" + q.trim().toLowerCase(Locale.ROOT) + "%"));
                    return cb.and(conditions.toArray(Predicate[]::new));
                };
        return menuItems.findAll(filter, Sort.by(Sort.Direction.DESC, "id")).stream()
                .sorted(
                        Comparator.comparing(
                                (MenuItem item) -> item.getStatus() != MenuItemStatus.ON_SALE))
                .map(MenuMapper::toResponse)
                .toList();
    }

    @Transactional
    public MenuItemResponse create(SaveMenuItemRequest r) {
        return MenuMapper.toResponse(menuItems.save(new MenuItem(r.name(), r.price())));
    }

    @Transactional
    public MenuItemResponse update(Long id, SaveMenuItemRequest r) {
        MenuItem item = item(id);
        item.updateInfo(r.name(), r.price());
        return MenuMapper.toResponse(item);
    }

    @Transactional
    public MenuItemResponse discontinue(Long id) {
        MenuItem item = item(id);
        item.discontinue();
        return MenuMapper.toResponse(item);
    }

    @Transactional
    public MenuItemResponse reactivate(Long id) {
        MenuItem item = item(id);
        item.activate();
        return MenuMapper.toResponse(item);
    }

    public List<RecipeLineResponse> recipe(Long id) {
        item(id);
        return recipes.findByMenuItemId(id).stream().map(MenuMapper::toRecipeLine).toList();
    }

    @Transactional
    public List<RecipeLineResponse> saveRecipe(Long id, SaveRecipeRequest request) {
        // Serialize complete replacements on the parent, including an initially empty recipe.
        MenuItem item =
                menuItems
                        .findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND, "Không tìm thấy món ăn"));
        var ids = new LinkedHashSet<Long>();
        for (RecipeLineRequest line : request.lines()) {
            if (!ids.add(line.ingredientId()))
                throw new BusinessException(ErrorCode.DUPLICATE_RECIPE_INGREDIENT);
            if (line.quantityPerServing() == null || line.quantityPerServing().signum() <= 0)
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Định lượng phải lớn hơn 0");
        }
        List<RecipeItem> existing = recipes.findByMenuItemId(id);
        var existingIds = new HashSet<Long>();
        for (RecipeItem line : existing) existingIds.add(line.getIngredient().getId());
        List<Ingredient> selected = ingredients.findAllById(ids);
        if (selected.size() != ids.size())
            throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy nguyên vật liệu");
        var byId = new HashMap<Long, Ingredient>();
        for (Ingredient ingredient : selected) {
            if (!existingIds.contains(ingredient.getId())
                    && ingredient.getStatus() != IngredientStatus.ACTIVE)
                throw new BusinessException(
                        ErrorCode.INGREDIENT_INACTIVE,
                        "Nguyên vật liệu đã ngừng sử dụng: " + ingredient.getName());
            byId.put(ingredient.getId(), ingredient);
        }
        var retained = new HashMap<Long, RecipeItem>();
        var removed = new ArrayList<RecipeItem>();
        for (RecipeItem line : existing) {
            if (ids.contains(line.getIngredient().getId()))
                retained.put(line.getIngredient().getId(), line);
            else removed.add(line);
        }
        recipes.deleteAll(removed);
        var added = new ArrayList<RecipeItem>();
        var result = new ArrayList<RecipeItem>();
        for (RecipeLineRequest line : request.lines()) {
            RecipeItem old = retained.get(line.ingredientId());
            if (old != null) {
                old.changeQuantity(line.quantityPerServing());
                result.add(old);
            } else {
                var created =
                        new RecipeItem(
                                item, byId.get(line.ingredientId()), line.quantityPerServing());
                added.add(created);
                result.add(created);
            }
        }
        recipes.saveAll(added);
        recipes.flush();
        return result.stream()
                .sorted(Comparator.comparing(r -> r.getIngredient().getName()))
                .map(MenuMapper::toRecipeLine)
                .toList();
    }

    private MenuItem item(Long id) {
        return menuItems
                .findById(id)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy món ăn"));
    }
}
