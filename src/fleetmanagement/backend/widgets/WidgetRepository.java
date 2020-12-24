package fleetmanagement.backend.widgets;

import fleetmanagement.backend.repositories.Repository;

import java.util.UUID;

public interface WidgetRepository extends Repository<Widget, UUID> {

    void insertOrReplace(Widget widget);

    Widget findWidgetByIndicatorId(String indicatorId);
    
    void  deleteWidgetByIndicatorId(String indicatorId);
}
