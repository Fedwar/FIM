package fleetmanagement.backend.events;

import java.util.Map;

public interface Event {
    Object getTarget();
    EventType getType();
    Map<String, Object> getProperties();
}
