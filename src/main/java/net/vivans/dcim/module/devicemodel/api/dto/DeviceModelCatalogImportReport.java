package net.vivans.dcim.module.devicemodel.api.dto;

import java.util.List;

public record DeviceModelCatalogImportReport(List<String> created, List<String> updated,
                                             List<String> skipped, List<String> disabledPoints,
                                             List<String> warnings, List<Error> errors) {
    public record Error(String model, String field, String message) {}
}
