package fleetmanagement.backend.diagnosis;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class Diagnosis implements Cloneable {
    private static final String BROKEN_STATUS = DeviceStatus.DEFECT.toString();
    private static final String DEGRADED_STATUS = DeviceStatus.DEGRADED.toString();
    private static final String UNKNOWN_STATUS = DeviceStatus.UNKNOWN.toString();

    private ZonedDateTime lastUpdated;
    private final UUID vehicleId;
    private final List<DiagnosedDevice> devices = new ArrayList<>();

    public Diagnosis(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Diagnosis(UUID vehicleId, ZonedDateTime lastUpdated, List<DiagnosedDevice> devices) {
        this.vehicleId = vehicleId;
        this.lastUpdated = lastUpdated;
        this.devices.addAll(devices);
    }

    public Diagnosis clone() {
        List<DiagnosedDevice> clonedDevices = devices.stream().map(DiagnosedDevice::clone).collect(toList());
        return new Diagnosis(vehicleId, lastUpdated, clonedDevices);
    }

//    public void integrate(Snapshot snapshot) {
//        lastUpdated = snapshot.timestamp;
//        integrateExistingDevices(snapshot);
//        disableMissingDevices(snapshot);
//    }

//    private void integrateExistingDevices(Snapshot snapshot) {
//        for (DeviceSnapshot componentSnapshot : snapshot.devices) {
//            DiagnosedDevice diagnosed = getOrCreateDevice(componentSnapshot.id);
//            diagnosed.integrate(componentSnapshot, snapshot.timestamp, snapshot.version);
//        }
//    }

    private DiagnosedDevice getOrCreateDevice(String id) {
        DiagnosedDevice diagnosed = getDevice(id);
        if (diagnosed == null) {
            diagnosed = new DiagnosedDevice(id);
            devices.add(diagnosed);
        }
        return diagnosed;
    }

    private void disableMissingDevices(Snapshot snapshot) {
        devices.stream().filter(x -> !snapshot.hasDevice(x.getId())).forEach(DiagnosedDevice::disable);
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public List<DiagnosedDevice> getDevices() {
        return devices;
    }

    void updateDevice(DiagnosedDevice diagnosedDevice) {
        devices.removeIf(d -> d.getId().equals(diagnosedDevice.getId()));
        devices.add(diagnosedDevice);
    }

    public DiagnosedDevice getDevice(String id) {
        return devices.stream().
                filter(c -> c.getId().equals(id)).
                findFirst().orElse(null);
    }

    public int countBrokenDevices() {
        return (int) getDevices().stream().filter(this::isBroken).count();
    }

    public int countDegradedDevices() {
        return (int) getDevices().stream().filter(this::isDegraded).count();
    }

    public int countUnknownDevices() {
        return (int) getDevices().stream().filter(this::isUnknown).count();
    }

    public List<DiagnosedDevice> getBrokenDevices() {
        return getDevices().stream().filter(this::isBroken).collect(toList());
    }

    public boolean isBroken(DiagnosedDevice x) {
        String status = x.getStatus();
        if (status != null) {
            return status.equalsIgnoreCase(BROKEN_STATUS);
        } else {
            List<StateEntry> state = x.getCurrentState();
            return state.stream().anyMatch(s -> s.category == ErrorCategory.FATAL
                    || s.category == ErrorCategory.SEVERE
                    || s.category == ErrorCategory.ERROR
            );
        }
    }

    public boolean isDegraded(DiagnosedDevice x) {
        String status = x.getStatus();
        if (status != null)
            return x.getStatus().equalsIgnoreCase(DEGRADED_STATUS);
        else
            return false;
    }

    public boolean isUnknown(DiagnosedDevice x) {
        String status = x.getStatus();
        if (status != null)
            return x.getStatus().equalsIgnoreCase(UNKNOWN_STATUS);
        else
            return false;
    }

    public void setLastUpdated(ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
