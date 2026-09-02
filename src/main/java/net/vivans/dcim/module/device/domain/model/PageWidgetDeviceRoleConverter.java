package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PageWidgetDeviceRoleConverter implements AttributeConverter<PageWidgetDeviceRole, String> {

    @Override
    public String convertToDatabaseColumn(PageWidgetDeviceRole attribute) {
        if (attribute == null || attribute == PageWidgetDeviceRole.DEFAULT) {
            return null;
        }
        return attribute.wireValue();
    }

    @Override
    public PageWidgetDeviceRole convertToEntityAttribute(String dbData) {
        return PageWidgetDeviceRole.from(dbData);
    }
}
