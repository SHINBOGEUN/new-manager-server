package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.PageWidgetLayoutRequest;
import net.vivans.dcim.module.device.domain.model.DevicePageCodes;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PageWidgetSpecializedSupport {

    private static final String DUPLICATE_NAME_MESSAGE = "widget name already exists on this page";

    private final PageWidgetRepository pageWidgetRepository;
    private final CommonCodeRepository commonCodeRepository;

    public CommonCode findPageCode(String pageCode) {
        if (pageCode == null || pageCode.isBlank()) {
            throw new IllegalArgumentException("pageCode is required");
        }
        return commonCodeRepository.findByCodeGroupGroupKeyAndCode(
                        DevicePageCodes.DEVICE_PAGE_GROUP_KEY, pageCode.trim())
                .orElseThrow(() -> new EntityNotFoundException("DEVICE_PAGE code not found: " + pageCode.trim()));
    }

    public PageWidget findWidget(Integer id) {
        return pageWidgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PageWidget not found: " + id));
    }

    public void validateCreateName(CommonCode pageCode, String name) {
        if (pageWidgetRepository.existsByPageCodeIdAndName(pageCode.getId(), name)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }
    }

    public void validateUpdateName(PageWidget widget, String name) {
        if (pageWidgetRepository.existsByPageCodeIdAndNameAndIdNot(
                widget.getPageCode().getId(), name, widget.getId())) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }
    }

    public void applyLayout(PageWidget widget, PageWidgetLayoutRequest layout) {
        if (layout != null) {
            widget.upsertLayout(layout.gridX(), layout.gridY(), layout.w(), layout.h());
        }
    }
}
