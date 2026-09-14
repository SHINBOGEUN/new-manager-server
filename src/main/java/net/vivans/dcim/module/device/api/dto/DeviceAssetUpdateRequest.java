package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record DeviceAssetUpdateRequest(
        @Schema(description = "내부 자산번호", example = "PDU-001") String assetCode,
        @Schema(description = "제조사 시리얼번호", example = "SN-123456") String serialNumber,
        @Schema(description = "자산 표시 색상(hex)", example = "#2563eb") String assetColor,
        @Schema(description = "장비 설명") String description,
        @Schema(description = "설치일", example = "2026-09-14") LocalDate installedDate,
        @Schema(description = "자산 담당자") String assetManagerName,
        @Schema(description = "공급사") String supplierName,
        @Schema(description = "보증 만료일", example = "2029-09-13") LocalDate warrantyExpiresOn
) {
}
