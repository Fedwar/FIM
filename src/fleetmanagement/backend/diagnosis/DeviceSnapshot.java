package fleetmanagement.backend.diagnosis;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.collections4.ListUtils.defaultIfNull;

public class DeviceSnapshot {

    public final String id;
    public final String location;
    public final LocalizedString name;
    public final String type;
    public final VersionInfo versions;
    public final String status;
    public final List<StateSnapshot> states;
    public final List<StateSnapshot> statesHistory;

    public DeviceSnapshot(String id, String location, LocalizedString name, String type, VersionInfo versions
            , String status, List<StateSnapshot> states, List<StateSnapshot> statesHistory) {
        this.id = id;
        this.location = location;
        this.name = name;
        this.type = type;
        this.versions = versions;
        this.status = status;
        this.states = defaultIfNull(states, Collections.EMPTY_LIST);
        this.statesHistory = defaultIfNull(statesHistory, Collections.EMPTY_LIST);
    }

    public DeviceSnapshot(String id, String location, String name, String type, VersionInfo versions
            , StateSnapshot state) {
        this(id, location, new LocalizedString(name), type, versions, null
                , state == null ? Collections.emptyList() : Collections.singletonList(state)
                , null
        );
    }

    public static class StateSnapshot {
        public final LocalizedString description;
        public final String code;
        public final ErrorCategory type;
        public final ZonedDateTime start;
        public final ZonedDateTime end;

        public StateSnapshot(LocalizedString description, String code, ErrorCategory type, ZonedDateTime start, ZonedDateTime end) {
            this.description = description;
            this.code = code;
            this.type = type;
            this.start = start;
            this.end = end;
        }

        public StateSnapshot(String description, String code, ErrorCategory type) {
            this(new LocalizedString(description), code, type, null, null);
        }

        public StateEntry toStateEntry()  {
            return new StateEntry(start, end, code, type, description );
        }
    }

}
