package fleetmanagement.backend.widgets;

import fleetmanagement.backend.notifications.settings.Type;
import gsp.util.DoNotObfuscate;

@DoNotObfuscate
public enum WidgetType {
    GAUGE {
        @Override
        public String getResourceKey() {
            return "widget_type_gauge";
        }
    }, BAR {
        @Override
        public String getResourceKey() {
            return "widget_type_bar";
        }
    }, CHART {
        @Override
        public String getResourceKey() {
            return "widget_type_chart";
        }
    };

    public abstract String getResourceKey();

    public static WidgetType getByResourceKey(String resourceKey) {
        for (WidgetType type : values()) {
            if (type.getResourceKey().equals(resourceKey)) {
                return type;
            }
        }
        return null;
    }
}
