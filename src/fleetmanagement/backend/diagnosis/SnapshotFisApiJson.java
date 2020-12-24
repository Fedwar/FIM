package fleetmanagement.backend.diagnosis;

import com.google.gson.internal.LinkedTreeMap;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@DoNotObfuscate
public class SnapshotFisApiJson {
    List<ComponentSnapshotJson> components;
    private static final Logger logger = Logger.getLogger(SnapshotFisApiJson.class);

    public Snapshot toSnapshot(UUID vehicleId) {
        List<DeviceSnapshot> parsed = components.stream().map(c -> c.toComponentSnapshot()).collect(Collectors.toList());
        return new Snapshot(vehicleId, 2, ZonedDateTime.now(), parsed);
    }

    @DoNotObfuscate
    public static class ComponentSnapshotJson {
        String id;
        String type;
        Map<String, String> name;
        String location;
        String status;
        List<StateJson> states;
        List<StateJson> status_history;
        LinkedTreeMap versions;

        public DeviceSnapshot toComponentSnapshot() {
            return new DeviceSnapshot(id, location, new LocalizedString(name), type,
                    new VersionInfo(versions),
                    status,
                    states == null ? null : states.stream().map(StateJson::toStatus).collect(Collectors.toList()),
                    status_history == null ? null : status_history.stream().map(StateJson::toStatus).collect(Collectors.toList()));
        }

    }

    @DoNotObfuscate
    public static class StateJson {
        String started;
        String ended;
        String type;
        Map<String, String> description;
        Integer code;

        public DeviceSnapshot.StateSnapshot toStatus() {
            return new DeviceSnapshot.StateSnapshot(new LocalizedString(description)
                    , code == null ? null : code.toString()
                    , type == null ? null : ErrorCategory.valueOf(type.toUpperCase())
                    , started == null ? null : ZonedDateTime.parse(started)
                    , ended == null ? null : ZonedDateTime.parse(ended));
        }
    }

}


