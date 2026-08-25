package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.PageWidgetCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetResponse;
import net.vivans.dcim.module.device.api.dto.PageWidgetUpdateRequest;
import net.vivans.dcim.module.device.domain.model.DevicePage;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetGroupBy;
import net.vivans.dcim.module.device.domain.model.PageWidgetOp;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageWidgetQueryService {

    private static final String DUPLICATE_NAME_MESSAGE = "widget name already exists on this page";

    private final PageWidgetRepository pageWidgetRepository;
    private final CommonCodeRepository commonCodeRepository;

    public List<PageWidgetResponse> getWidgets(String pageCode) {
        CommonCode code = findPageCode(pageCode);
        List<PageWidget> widgets = pageWidgetRepository.findAllByPageCodeIdOrderByIdAsc(code.getId());
        List<PageWidgetResponse> responses = new ArrayList<>();
        for (PageWidget widget : widgets) {
            responses.add(PageWidgetResponse.from(widget));
        }
        return responses;
    }

    public PageWidgetResponse getWidget(Integer id) {
        return PageWidgetResponse.from(findWidget(id));
    }

    @Transactional
    public PageWidgetResponse createWidget(PageWidgetCreateRequest request) {
        CommonCode pageCode = findPageCode(request.pageCode());
        if (pageWidgetRepository.existsByPageCodeIdAndName(pageCode.getId(), request.name().trim())) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }

        boolean enabled = request.enabled() == null || request.enabled();
        PageWidget widget = PageWidget.create(
                pageCode,
                request.name(),
                enabled,
                PageWidgetQueryKind.from(request.queryKind()),
                PageWidgetOp.from(request.op()),
                PageWidgetGroupBy.from(request.groupBy()),
                request.weightPoint(),
                request.numeratorPoint(),
                request.denominatorPoint(),
                request.pointNames()
        );
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse updateWidget(Integer id, PageWidgetUpdateRequest request) {
        PageWidget widget = findWidget(id);
        String name = request.name().trim();
        if (pageWidgetRepository.existsByPageCodeIdAndNameAndIdNot(widget.getPageCode().getId(), name, id)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }

        boolean enabled = request.enabled() == null ? widget.isEnabled() : request.enabled();
        widget.update(
                name,
                enabled,
                PageWidgetQueryKind.from(request.queryKind()),
                PageWidgetOp.from(request.op()),
                PageWidgetGroupBy.from(request.groupBy()),
                request.weightPoint(),
                request.numeratorPoint(),
                request.denominatorPoint(),
                request.pointNames()
        );
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public Integer deleteWidget(Integer id) {
        PageWidget widget = findWidget(id);
        pageWidgetRepository.delete(widget);
        return id;
    }

    private PageWidget findWidget(Integer id) {
        return pageWidgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PageWidget not found: " + id));
    }

    private CommonCode findPageCode(String pageCode) {
        if (pageCode == null || pageCode.isBlank()) {
            throw new IllegalArgumentException("pageCode is required");
        }
        return commonCodeRepository.findByCodeGroupGroupKeyAndCode(
                        DevicePage.DEVICE_PAGE_GROUP_KEY,
                        pageCode.trim()
                )
                .orElseThrow(() -> new EntityNotFoundException("DEVICE_PAGE code not found: " + pageCode.trim()));
    }
}
