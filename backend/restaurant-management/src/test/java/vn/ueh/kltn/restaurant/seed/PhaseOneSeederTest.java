package vn.ueh.kltn.restaurant.seed;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static vn.ueh.kltn.restaurant.TestFixtures.CLOCK;

import org.junit.jupiter.api.Test;

import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.DiningTableRepository;
import vn.ueh.kltn.restaurant.repository.IngredientRepository;
import vn.ueh.kltn.restaurant.repository.MenuItemRepository;
import vn.ueh.kltn.restaurant.repository.RecipeItemRepository;
import vn.ueh.kltn.restaurant.repository.ReservationRepository;
import vn.ueh.kltn.restaurant.service.ReservationService;

class PhaseOneSeederTest {
    @Test
    void independentMenuGateDoesNotDependOnExistingAccountsAndSecondRunDoesNotDuplicate() {
        MenuItemRepository menu = mock(MenuItemRepository.class);
        IngredientRepository ingredients = mock(IngredientRepository.class);
        RecipeItemRepository recipes = mock(RecipeItemRepository.class);
        ReservationRepository reservations = mock(ReservationRepository.class);
        AccountRepository accounts = mock(AccountRepository.class);
        DiningTableRepository tables = mock(DiningTableRepository.class);
        when(menu.count()).thenReturn(0L, 10L);
        when(ingredients.count()).thenReturn(10L);
        when(recipes.count()).thenReturn(14L);
        when(reservations.count()).thenReturn(3L);
        var p =
                new AppProperties(
                        "Asia/Ho_Chi_Minh",
                        true,
                        new AppProperties.Jwt("x".repeat(40), 12),
                        new AppProperties.Alert(3),
                        new AppProperties.Reservation(60));
        var seeder =
                new PhaseOneSeeder(
                        p,
                        menu,
                        ingredients,
                        recipes,
                        reservations,
                        tables,
                        accounts,
                        mock(ReservationService.class),
                        CLOCK);
        seeder.run();
        seeder.run();
        verify(menu, times(10)).save(any());
        verifyNoInteractions(accounts, tables);
        verify(ingredients, never()).saveAll(any());
        verify(recipes, never()).saveAll(any());
    }
}
