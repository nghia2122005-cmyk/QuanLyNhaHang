package vn.ueh.kltn.restaurant.service;

import vn.ueh.kltn.restaurant.dto.request.SaveDiningAreaRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveDiningTableRequest;
import vn.ueh.kltn.restaurant.dto.response.DiningAreaResponse;
import vn.ueh.kltn.restaurant.dto.response.DiningTableResponse;
import vn.ueh.kltn.restaurant.dto.response.FloorPlanAreaResponse;

import java.util.List;

public interface TableService {
    List<FloorPlanAreaResponse> floorPlan();

    List<DiningAreaResponse> listAreas();

    List<DiningTableResponse> listTables();

    DiningAreaResponse createArea(SaveDiningAreaRequest request);

    DiningAreaResponse updateArea(Long id, SaveDiningAreaRequest request);

    DiningAreaResponse deactivateArea(Long id);

    DiningAreaResponse reactivateArea(Long id);

    DiningTableResponse createTable(SaveDiningTableRequest request);

    DiningTableResponse updateTable(Long id, SaveDiningTableRequest request);

    DiningTableResponse deactivateTable(Long id);

    DiningTableResponse reactivateTable(Long id);
}
