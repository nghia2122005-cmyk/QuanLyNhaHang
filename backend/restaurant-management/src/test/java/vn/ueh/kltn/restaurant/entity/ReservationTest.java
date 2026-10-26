package vn.ueh.kltn.restaurant.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static vn.ueh.kltn.restaurant.TestFixtures.reservation;
import static vn.ueh.kltn.restaurant.TestFixtures.table;

import org.junit.jupiter.api.Test;

import vn.ueh.kltn.restaurant.enums.ReservationStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;

class ReservationTest {
    @Test
    void confirmSeatedIsStrict() {
        Reservation r = reservation(table(1L, 4));
        r.confirmSeated();
        assertEquals(ReservationStatus.SEATED, r.getStatus());
        assertEquals(
                ErrorCode.INVALID_STATE,
                assertThrows(BusinessException.class, r::confirmSeated).getErrorCode());
        assertThrows(BusinessException.class, r::cancel);
    }

    @Test
    void cancelIsIdempotentAndCannotSeatCancelled() {
        Reservation r = reservation(table(1L, 4));
        r.cancel();
        r.cancel();
        assertEquals(ReservationStatus.CANCELLED, r.getStatus());
        assertThrows(BusinessException.class, r::confirmSeated);
    }
}
