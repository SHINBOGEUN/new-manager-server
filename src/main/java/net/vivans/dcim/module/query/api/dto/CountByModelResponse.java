package net.vivans.dcim.module.query.api.dto;

public record CountByModelResponse(
        Integer modelId,
        String modelName,
        String manufacturer,
        int count
) {
}
