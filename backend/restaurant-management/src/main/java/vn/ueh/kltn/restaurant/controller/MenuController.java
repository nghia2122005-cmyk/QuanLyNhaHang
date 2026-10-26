package vn.ueh.kltn.restaurant.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

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

import vn.ueh.kltn.restaurant.dto.request.SaveMenuItemRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveRecipeRequest;
import vn.ueh.kltn.restaurant.dto.response.MenuItemResponse;
import vn.ueh.kltn.restaurant.dto.response.RecipeLineResponse;
import vn.ueh.kltn.restaurant.enums.MenuItemStatus;
import vn.ueh.kltn.restaurant.service.MenuService;

import java.util.List;

@RestController
@RequestMapping("/api/menu-items")
@RequiredArgsConstructor
public class MenuController {
    private final MenuService menu;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MenuItemResponse> list(
            @RequestParam(required = false) MenuItemStatus status,
            @RequestParam(required = false) String q) {
        return menu.list(status, q);
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public MenuItemResponse create(@Valid @RequestBody SaveMenuItemRequest r) {
        return menu.create(r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public MenuItemResponse update(
            @PathVariable Long id, @Valid @RequestBody SaveMenuItemRequest r) {
        return menu.update(id, r);
    }

    @PatchMapping("/{id}/discontinue")
    @PreAuthorize("hasRole('OWNER')")
    public MenuItemResponse discontinue(@PathVariable Long id) {
        return menu.discontinue(id);
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('OWNER')")
    public MenuItemResponse reactivate(@PathVariable Long id) {
        return menu.reactivate(id);
    }

    @GetMapping("/{id}/recipe")
    @PreAuthorize("hasAnyRole('OWNER','HEAD_CHEF')")
    public List<RecipeLineResponse> recipe(@PathVariable Long id) {
        return menu.recipe(id);
    }

    @PutMapping("/{id}/recipe")
    @PreAuthorize("hasAnyRole('OWNER','HEAD_CHEF')")
    public List<RecipeLineResponse> saveRecipe(
            @PathVariable Long id, @Valid @RequestBody SaveRecipeRequest r) {
        return menu.saveRecipe(id, r);
    }
}
