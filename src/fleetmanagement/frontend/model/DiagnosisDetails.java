package fleetmanagement.frontend.model;

import fleetmanagement.backend.diagnosis.DiagnosedDevice;
import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.diagnosis.StateEntry;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.I18n;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.transformers.DurationFormatter;
import org.apache.log4j.Logger;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class DiagnosisDetails {
    private static final Logger logger = Logger.getLogger(DiagnosisDetails.class);
    public final List<DeviceGroup> groups;
    public final String vehicleId;
    public final String vehicleName;
    public final String lastUpdated;
    public final int defectiveDevicesCount;
    public final int degradedDevicesCount;
    public final int unknownDevicesCount;
    public final int operationalDevicesCount;
    public final Licence licence;

    public DiagnosisDetails(Diagnosis diagnosis, Vehicle vehicle, Licence licence, UserSession session) {
        this.vehicleId = vehicle.id.toString();
        this.vehicleName = vehicle.getName();
        this.groups = new ArrayList<>();
        this.licence = licence;

        if (diagnosis == null)
            diagnosis = new Diagnosis(vehicle.id);

        this.lastUpdated = toHumanReadableDuration(diagnosis.getLastUpdated(), session);
        this.defectiveDevicesCount = diagnosis.countBrokenDevices();
        this.degradedDevicesCount = diagnosis.countDegradedDevices();
        this.unknownDevicesCount = diagnosis.countUnknownDevices();
        this.operationalDevicesCount = diagnosis.getDevices().size()
                - defectiveDevicesCount - degradedDevicesCount - unknownDevicesCount;

        Diagnosis finalDiagnosis = diagnosis;
        List<Device> devices = diagnosis.getDevices().stream()
                .map(x -> new Device(x, session, finalDiagnosis)).collect(toList());
        Map<String, List<Device>> devicesByType = devices.stream().collect(groupingBy(d -> d.type));

        for (Map.Entry<String, List<Device>> group : devicesByType.entrySet()) {
            groups.add(new DeviceGroup(group.getKey(), group.getValue(), session));
        }
        Comparator<DeviceGroup> byNumberOfDevices = Comparator.comparing(x -> x.devices.size());
        groups.sort(byNumberOfDevices.thenComparing(x -> x.deviceType));

    }

    public static class DeviceGroup {
        public final List<Device> devices;
        public final String deviceType;
        public final List<String> versions;
        public final int devicesWithErrors;
        public final int devicesWithDegradedStatus;
        public final int devicesWithUnknownStatus;

        public DeviceGroup(String type, List<Device> devices, UserSession session) {
            this.deviceType = type;
            this.devices = devices;
            this.devicesWithErrors = (int) devices.stream().filter(x -> x.isError).count();
            this.devicesWithDegradedStatus = (int) devices.stream().filter(x -> x.isDegraded).count();
            this.devicesWithUnknownStatus = (int) devices.stream().filter(x -> x.isUnknown).count();
            boolean allDevicesHaveTheSameVersion = devices.stream().map(x -> x.versions).distinct().limit(2).count() == 1;
            this.versions = allDevicesHaveTheSameVersion ?
                    devices.get(0).versions :
                    Collections.singletonList(I18n.get(session, "diagnosis_details_various_versions"));
        }

        public boolean containsMultipleDevices() {
            return devices.size() > 1;
        }
    }

    public static class Device {
        public final String id;
        public final String name;
        public final String location;
        public final List<String> versions;
        public final String type;
        @SuppressWarnings("WeakerAccess") public final boolean isError;
        @SuppressWarnings("WeakerAccess") public final boolean isDegraded;
        @SuppressWarnings("WeakerAccess") public final boolean isUnknown;
        @SuppressWarnings("WeakerAccess") public final boolean containsStateWithNonNullCode;
        public final List<DeviceState> currentState;

        public Device(DiagnosedDevice diagnosed, UserSession session, Diagnosis diagnosis) {
            String name = DiagnosisDetails.getDeviceName(diagnosed, session);

            this.id = diagnosed.getId();
            this.name = name;
            this.type = diagnosed.getType();
            this.location = diagnosed.getLocation();
            this.versions = diagnosed.getVersionsInfo().getAll().entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .map(e -> e.getKey()+": "+e.getValue())
                    .collect(Collectors.toList());
            this.isError = diagnosis.isBroken(diagnosed);
            this.isDegraded = diagnosis.isDegraded(diagnosed);
            this.isUnknown = diagnosis.isUnknown(diagnosed);

            List<StateEntry> deviceState = diagnosed.getCurrentState();
            currentState = deviceState == null ? new ArrayList<>() :
                    diagnosed.getCurrentState().stream()
                            .map(s -> new DeviceState(s, session))
                            .collect(Collectors.toList());

            this.containsStateWithNonNullCode = currentState.stream().anyMatch(ds -> ds.code != null && !ds.code.equals("0"));
        }
    }

    public static String getDeviceName(DiagnosedDevice diagnosed, UserSession session) {
        String name = diagnosed.getName().get(session.getAcceptableLanguages());
        if (name.isEmpty()) name = diagnosed.getName().get(Locale.ENGLISH);
        if (name.isEmpty()) {
            logger.error("Diagnosed device with id \"" + diagnosed.getId() + "\" has no name in any acceptable language");
            name = "-";
        }
        return name;
    }

    public static class DeviceState {
        public final String code;
        public final String message;
        public final String currentStatusSince;

        public DeviceState(StateEntry state, UserSession session) {
            this.message = state.message.get(session.getAcceptableLanguages());
            this.code = state.code;
            this.currentStatusSince = toHumanReadableDuration(state.start, session);
        }
    }

    private static String toHumanReadableDuration(ZonedDateTime lastUpdated, UserSession session) {
        if (lastUpdated == null)
            return "";
        ZonedDateTime now = ZonedDateTime.now();
        return DurationFormatter.asHumanReadable(Duration.between(now, lastUpdated), session.getLocale());
    }

}
