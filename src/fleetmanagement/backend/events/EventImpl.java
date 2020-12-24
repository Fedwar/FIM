package fleetmanagement.backend.events;

import java.util.HashMap;
import java.util.Map;

public class EventImpl implements Event {

    private Object target;
    private EventType type;
    private Map<String, Object> properties;

    public EventImpl(Object target, EventType type, Map<String, Object> properties)
    {
        this.target = target;
        this.type = type;
        this.properties = properties;
    }

    public EventImpl(Object target, EventType type)
    {
        this(target, type, new HashMap<>());
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public EventType getType() {
        return type;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }
}