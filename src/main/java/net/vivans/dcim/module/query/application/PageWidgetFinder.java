package net.vivans.dcim.module.query.application;

import jakarta.persistence.EntityNotFoundException;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;

public final class PageWidgetFinder {

    private PageWidgetFinder() {
    }

    public static PageWidget findRequired(PageWidgetRepository pageWidgetRepository, Integer widgetId) {
        if (widgetId == null) {
            throw new IllegalArgumentException("widgetId is required");
        }
        return pageWidgetRepository.findById(widgetId)
                .orElseThrow(() -> new EntityNotFoundException("PageWidget not found: " + widgetId));
    }
}
