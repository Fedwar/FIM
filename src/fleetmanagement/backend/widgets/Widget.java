package fleetmanagement.backend.widgets;

import fleetmanagement.backend.repositories.Persistable;

import java.util.UUID;

public class Widget implements Persistable<UUID> {
    public final UUID id;
    public final String indicatorId;
    public final Object maxValue;
    public final Object minValue;
    public final WidgetType type;

    public Widget(UUID id, String indicatorId, Object maxValue, Object minValue, WidgetType type) {
        this.id = id;
        this.indicatorId = indicatorId;
        this.maxValue = convertValue(maxValue);
        this.minValue = convertValue(minValue);
        this.type = type;
    }

    public Widget(String indicatorId, Object maxValue, Object minValue, WidgetType type) {
        this(UUID.randomUUID(), indicatorId, maxValue, minValue, type);
    }

    private Object convertValue(Object value) {
        if (value == null)
            return null;
        if (value instanceof String && ((String) value).trim().isEmpty())
            return null;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ignored) {
        }

        return value;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Widget clone() {
        try {
            return (Widget) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
