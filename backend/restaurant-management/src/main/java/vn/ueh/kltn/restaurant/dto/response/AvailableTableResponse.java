package vn.ueh.kltn.restaurant.dto.response;

public record AvailableTableResponse(
        Long id, String name, int capacity, String areaName, String code) {}
