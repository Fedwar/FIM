package fleetmanagement.backend.diagnosis;

import fleetmanagement.backend.diagnosis.VersionInfo.VersionType;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class DiagnosedDevice implements Cloneable {
    private final String id;
    private String type;
    private LocalizedString name;
    private String location;
    private String status;
    private List<StateEntry> states;
    private ErrorHistory errorHistory;
    private VersionInfo versions;
    private boolean disabled = false;

    public DiagnosedDevice(String id) {
        this(id, null, null, null, null, null
                , false, null, null);
    }

    public DiagnosedDevice(String id, String location, LocalizedString name, String type, String status
            , List<StateEntry> states, boolean disabled, VersionInfo versions, ErrorHistory errorHistory) {
        this.id = id;
        this.location = location;
        this.name = defaultIfNull(name, new LocalizedString());
        this.type = type;
        this.status = status;
        this.states = defaultIfNull(states, new ArrayList<>());
        this.disabled = disabled;
        this.versions = defaultIfNull(versions, new VersionInfo());
        this.errorHistory = defaultIfNull(errorHistory, new ErrorHistory());
    }

    public DiagnosedDevice clone() {
        return new DiagnosedDevice(id, location, name, type, status, states, disabled,
                versions == null ? null : versions.clone(),
                errorHistory.clone());
    }

//
//    public void integrate(DeviceSnapshot snapshot, ZonedDateTime timestamp, int snapshotVersion) {
//        this.disabled = false;
//        this.type = snapshot.type;
//        this.name = snapshot.name;
//        this.location = snapshot.location;
//        this.versions = snapshot.versions;
//        this.status = snapshot.status;
//
//        if (snapshotVersion == 1) {
//            states = snapshot.states.stream()
//                    .map(s -> new StateEntry(timestamp, null, s.code, s.type, s.description))
//                    .collect(Collectors.toList());
//
//            errorHistory.integrate(snapshot.states.get(0), timestamp);
//        } else {
//            states = snapshot.states.stream()
//                    .map(DeviceSnapshot.StateSnapshot::toStateEntry)
//                    .collect(Collectors.toList());
//            errorHistory.integrate(snapshot.statesHistory);
//        }
//    }


    public void disable() {
        this.disabled = true;
    }

    public String getId() {
        return id;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public String getLocation() {
        return location;
    }

    public LocalizedString getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getVersion(VersionType type) {
        return versions == null ? null : versions.get(type);
    }

    public VersionInfo getVersionsInfo() {
        return versions;
    }

    public List<StateEntry> getCurrentState() {
        return states;
    }

    @Deprecated
    public ErrorHistory getErrorHistory() {
        return errorHistory;
    }

    public String getStatus() {
        return status;
    }
}
