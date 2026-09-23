package net.vivans.dcim.module.lora.api.dto;

import java.util.List;

public record LoraCollectionStatusResponse(
        LoraCollectionStatusSummary summary,
        LoraCollectionSourceSummary sourceSummary,
        List<LoraCollectionStatusRow> rows
) {
}
