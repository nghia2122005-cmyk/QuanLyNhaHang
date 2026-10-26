package vn.ueh.kltn.restaurant.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

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

import vn.ueh.kltn.restaurant.dto.request.CreateReservationRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveDiningAreaRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveDiningTableRequest;
import vn.ueh.kltn.restaurant.dto.response.AvailableTableResponse;
import vn.ueh.kltn.restaurant.dto.response.DiningAreaResponse;
import vn.ueh.kltn.restaurant.dto.response.DiningTableResponse;
import vn.ueh.kltn.restaurant.dto.response.FloorPlanAreaResponse;
import vn.ueh.kltn.restaurant.dto.response.PageResponse;
import vn.ueh.kltn.restaurant.dto.response.ReservationResponse;
import vn.ueh.kltn.restaurant.enums.ReservationStatus;
import vn.ueh.kltn.restaurant.service.ReservationService;
import vn.ueh.kltn.restaurant.service.TableService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TableController {
    private final TableService tables;
    private final ReservationService reservations;

    @GetMapping("/areas")
    @PreAuthorize("hasRole('OWNER')")
    public List<DiningAreaResponse> areas() {
        return tables.listAreas();
    }

    @PostMapping("/areas")
    @PreAuthorize("hasRole('OWNER')")
    public DiningAreaResponse createArea(@Valid @RequestBody SaveDiningAreaRequest request) {
        return tables.createArea(request);
    }

    @PutMapping("/areas/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public DiningAreaResponse updateArea(
            @PathVariable Long id, @Valid @RequestBody SaveDiningAreaRequest request) {
        return tables.updateArea(id, request);
    }

    @PatchMapping("/areas/{id}/deactivate")
    @PreAuthorize("hasRole('OWNER')")
    public DiningAreaResponse deactivateArea(@PathVariable Long id) {
        return tables.deactivateArea(id);
    }

    @PatchMapping("/areas/{id}/reactivate")
    @PreAuthorize("hasRole('OWNER')")
    public DiningAreaResponse reactivateArea(@PathVariable Long id) {
        return tables.reactivateArea(id);
    }

    @GetMapping("/tables")
    @PreAuthorize("hasRole('OWNER')")
    public List<DiningTableResponse> managedTables() {
        return tables.listTables();
    }

    @PostMapping("/tables")
    @PreAuthorize("hasRole('OWNER')")
    public DiningTableResponse createTable(@Valid @RequestBody SaveDiningTableRequest request) {
        return tables.createTable(request);
    }

    @PutMapping("/tables/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public DiningTableResponse updateTable(
            @PathVariable Long id, @Valid @RequestBody SaveDiningTableRequest request) {
        return tables.updateTable(id, request);
    }

    @PatchMapping("/tables/{id}/deactivate")
    @PreAuthorize("hasRole('OWNER')")
    public DiningTableResponse deactivateTable(@PathVariable Long id) {
        return tables.deactivateTable(id);
    }

    @PatchMapping("/tables/{id}/reactivate")
    @PreAuthorize("hasRole('OWNER')")
    public DiningTableResponse reactivateTable(@PathVariable Long id) {
        return tables.reactivateTable(id);
    }

    @GetMapping("/tables/floor-plan")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','CASHIER','OWNER')")
    public List<FloorPlanAreaResponse> floorPlan() {
        return tables.floorPlan();
    }

    @GetMapping("/reservations")
    @PreAuthorize("hasAnyRole('MANAGER','WAITER','OWNER')")
    public PageResponse<ReservationResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @Positive(message = "Mã bàn phải lớn hơn 0")
                    Long tableId,
            @RequestParam(defaultValue = "false") boolean unexpired,
            @RequestParam(defaultValue = "0")
                    @Min(value = 0, message = "Số trang phải lớn hơn hoặc bằng 0")
                    int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Kích thước trang phải lớn hơn 0")
                    @Max(value = 100, message = "Mỗi trang tối đa 100 dòng")
                    int size) {
        return reservations.list(date, status, q, tableId, unexpired, page, size);
    }

    @GetMapping("/reservations/available-tables")
    @PreAuthorize("hasRole('MANAGER')")
    public List<AvailableTableResponse> available(
            @RequestParam Instant startTime,
            @RequestParam Instant endTime,
            @RequestParam @Min(value = 1, message = "Số khách phải lớn hơn 0") int guestCount,
            @RequestParam(required = false) @Positive Long excludedId) {
        return reservations.available(startTime, endTime, guestCount, excludedId);
    }

    @PostMapping("/reservations")
    @PreAuthorize("hasRole('MANAGER')")
    public ReservationResponse create(@Valid @RequestBody CreateReservationRequest request) {
        return reservations.create(request);
    }

    @PutMapping("/reservations/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ReservationResponse update(
            @PathVariable Long id, @Valid @RequestBody CreateReservationRequest request) {
        return reservations.update(id, request);
    }

    @PatchMapping("/reservations/{id}/cancel")
    @PreAuthorize("hasRole('MANAGER')")
    public ReservationResponse cancel(@PathVariable Long id) {
        return reservations.cancel(id);
    }
}
