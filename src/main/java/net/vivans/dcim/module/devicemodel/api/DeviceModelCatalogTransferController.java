package net.vivans.dcim.module.devicemodel.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelCatalogDocument;
import net.vivans.dcim.module.devicemodel.application.DeviceModelCatalogTransferService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/device-models")
public class DeviceModelCatalogTransferController {
    private static final int MAX_IMPORT_BYTES = 2 * 1024 * 1024;
    private final DeviceModelCatalogTransferService transferService;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportCatalog(@RequestParam(required = false) List<Integer> ids) throws JsonProcessingException {
        byte[] json = objectMapper.writeValueAsBytes(transferService.exportCatalog(ids));
        String filename = "dcim-models-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(json.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(json);
    }

    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> importCatalog(
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(defaultValue = "SKIP") String onConflict,
            @RequestBody byte[] body) {
        if (body.length > MAX_IMPORT_BYTES) throw new IllegalArgumentException("catalog JSON must be 2 MiB or smaller");
        final DeviceModelCatalogDocument document;
        try {
            document = objectMapper.readValue(body, DeviceModelCatalogDocument.class);
        } catch (IOException ex) {
            String message = ex instanceof JsonProcessingException jsonError ? jsonError.getOriginalMessage() : ex.getMessage();
            throw new IllegalArgumentException("invalid catalog JSON: " + message, ex);
        }
        return ResponseEntity.ok(ApiResponse.ok(transferService.importCatalog(document, dryRun, onConflict)));
    }
}
