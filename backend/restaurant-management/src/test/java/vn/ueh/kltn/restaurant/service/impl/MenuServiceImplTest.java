package vn.ueh.kltn.restaurant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static vn.ueh.kltn.restaurant.TestFixtures.id;
import static vn.ueh.kltn.restaurant.TestFixtures.ingredient;
import static vn.ueh.kltn.restaurant.TestFixtures.menu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import vn.ueh.kltn.restaurant.dto.request.RecipeLineRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveRecipeRequest;
import vn.ueh.kltn.restaurant.dto.response.RecipeLineResponse;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.entity.MenuItem;
import vn.ueh.kltn.restaurant.entity.RecipeItem;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.repository.IngredientRepository;
import vn.ueh.kltn.restaurant.repository.MenuItemRepository;
import vn.ueh.kltn.restaurant.repository.RecipeItemRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class MenuServiceImplTest {
    private final MenuItemRepository menus = mock(MenuItemRepository.class);
    private final RecipeItemRepository recipes = mock(RecipeItemRepository.class);
    private final IngredientRepository ingredients = mock(IngredientRepository.class);
    private MenuServiceImpl service;
    private final MenuItem menu = menu();

    @BeforeEach
    void setup() {
        service = new MenuServiceImpl(menus, recipes, ingredients);
        when(menus.findLockedById(1L)).thenReturn(Optional.of(menu));
    }

    private RecipeLineRequest line(Long id, String quantity) {
        return new RecipeLineRequest(id, new BigDecimal(quantity));
    }

    @Test
    void menuUsesNewestIdWithinStatusAndKeepsOnSaleAheadOfDiscontinued() {
        MenuItem stopped = id(new MenuItem("Đã ngừng", BigDecimal.ONE), 30L);
        stopped.discontinue();
        MenuItem recent = id(new MenuItem("Mới", BigDecimal.ONE), 20L);
        MenuItem older = id(new MenuItem("Cũ", BigDecimal.ONE), 10L);
        when(menus.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(stopped, recent, older));
        assertEquals(
                List.of(20L, 10L, 30L),
                service.list(null, null).stream().map(item -> item.id()).toList());
        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(menus).findAll(any(Specification.class), sort.capture());
        assertEquals(Sort.Direction.DESC, sort.getValue().getOrderFor("id").getDirection());
    }

    @Test
    void diffDeletesUpdatesAndAddsWithoutReinsertingRetainedKey() {
        Ingredient keptIngredient = ingredient(1L);
        Ingredient removedIngredient = ingredient(2L);
        Ingredient addedIngredient = ingredient(3L);
        var kept = new RecipeItem(menu, keptIngredient, BigDecimal.ONE);
        var removed = new RecipeItem(menu, removedIngredient, BigDecimal.ONE);
        when(recipes.findByMenuItemId(1L)).thenReturn(List.of(kept, removed));
        when(ingredients.findAllById(any())).thenReturn(List.of(keptIngredient, addedIngredient));
        List<RecipeLineResponse> result =
                service.saveRecipe(
                        1L, new SaveRecipeRequest(List.of(line(1L, "0.250"), line(3L, "0.500"))));
        assertEquals(new BigDecimal("0.250"), kept.getQuantityPerServing());
        assertEquals(2, result.size());
        verify(recipes).deleteAll(List.of(removed));
        verify(recipes)
                .saveAll(
                        argThat(
                                lines -> {
                                    var all = new ArrayList<RecipeItem>();
                                    lines.forEach(all::add);
                                    return all.size() == 1
                                            && all.getFirst().getIngredient().getId().equals(3L);
                                }));
        verify(recipes).flush();
        InOrder order = inOrder(menus, recipes);
        order.verify(menus).findLockedById(1L);
        order.verify(recipes).findByMenuItemId(1L);
    }

    @Test
    void retainedInactiveIngredientDoesNotBlockEditingAnotherLine() {
        Ingredient inactive = ingredient(1L);
        inactive.deactivate();
        Ingredient active = ingredient(2L);
        RecipeItem retained = new RecipeItem(menu, inactive, BigDecimal.ONE);
        RecipeItem changed = new RecipeItem(menu, active, BigDecimal.ONE);
        when(recipes.findByMenuItemId(1L)).thenReturn(List.of(retained, changed));
        when(ingredients.findAllById(any())).thenReturn(List.of(inactive, active));

        assertEquals(
                2,
                service.saveRecipe(
                                1L,
                                new SaveRecipeRequest(List.of(line(1L, "1"), line(2L, "0.250"))))
                        .size());
        assertEquals(new BigDecimal("0.250"), changed.getQuantityPerServing());
        assertEquals(BigDecimal.ONE, retained.getQuantityPerServing());
        verify(recipes).saveAll(List.of());
    }

    @Test
    void missingMenuStopsBeforeReadingOrChangingRecipe() {
        when(menus.findLockedById(1L)).thenReturn(Optional.empty());
        assertEquals(
                ErrorCode.NOT_FOUND,
                assertThrows(
                                BusinessException.class,
                                () -> service.saveRecipe(1L, new SaveRecipeRequest(List.of())))
                        .getErrorCode());
        verifyNoInteractions(recipes, ingredients);
    }

    @Test
    void emptyLinesRemoveAll() {
        var old = new RecipeItem(menu, ingredient(1L), BigDecimal.ONE);
        when(recipes.findByMenuItemId(1L)).thenReturn(List.of(old));
        assertTrue(service.saveRecipe(1L, new SaveRecipeRequest(List.of())).isEmpty());
        verify(recipes).deleteAll(List.of(old));
    }

    @Test
    void duplicateIngredientIs400() {
        BusinessException failure =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.saveRecipe(
                                        1L,
                                        new SaveRecipeRequest(
                                                List.of(line(1L, "1"), line(1L, "2")))));
        assertEquals(ErrorCode.DUPLICATE_RECIPE_INGREDIENT, failure.getErrorCode());
        verify(recipes, never()).deleteAll(any());
    }

    @Test
    void inactiveIngredientIs400() {
        Ingredient i = ingredient(1L);
        i.deactivate();
        when(ingredients.findAllById(any())).thenReturn(List.of(i));
        assertEquals(
                ErrorCode.INGREDIENT_INACTIVE,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service.saveRecipe(
                                                1L, new SaveRecipeRequest(List.of(line(1L, "1")))))
                        .getErrorCode());
    }

    @Test
    void nonpositiveQuantityIsRejected() {
        for (String q : List.of("0", "-1"))
            assertEquals(
                    ErrorCode.INVALID_INPUT,
                    assertThrows(
                                    BusinessException.class,
                                    () ->
                                            service.saveRecipe(
                                                    1L,
                                                    new SaveRecipeRequest(List.of(line(1L, q)))))
                            .getErrorCode());
    }

    @Test
    void missingIngredientIs404() {
        assertEquals(
                ErrorCode.NOT_FOUND,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service.saveRecipe(
                                                1L, new SaveRecipeRequest(List.of(line(99L, "1")))))
                        .getErrorCode());
    }
}
