package fleetmanagement.frontend.model;

import fleetmanagement.backend.diagnosis.DiagnosedDevice;
import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.diagnosis.DiagnosisRepository;
import fleetmanagement.backend.diagnosis.StateEntry;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.transformers.DurationFormatter;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DiagnosedDeviceModel {

    public final String vehicleId;
    public final String deviceName;
    public final List<StatusEntryModel> errors;
    public final DiagnosedDevice device;
    public final String updated;
    public final Licence licence;
    private final UserSession session;

    public DiagnosedDeviceModel(DiagnosisRepository diagnosisRepository, UUID vehicleId, String deviceId, UserSession session, Licence licence) {
        this.session = session;
        Diagnosis diagnosis = diagnosisRepository.tryFindByVehicleId(vehicleId);
        device = diagnosis.getDevice(deviceId);
        updated = diagnosis.getLastUpdated() != null ? toHumanReadableDuration(diagnosis.getLastUpdated(), session) : "";
        this.vehicleId = diagnosis.getVehicleId().toString();
        deviceName = device.getName().get(session.getAcceptableLanguages());
        errors = diagnosisRepository.getDiagnosedDeviceHistory(vehicleId, deviceId).stream()
                .map(StatusEntryModel::new)
                .collect(Collectors.toList());
        this.licence = licence;
    }

    private static String toHumanReadableDuration(ZonedDateTime lastUpdated, UserSession session) {
        ZonedDateTime now = ZonedDateTime.now();
        return DurationFormatter.asHumanReadable(Duration.between(now, lastUpdated), session.getLocale());
    }

    public class StatusEntryModel {
        public final ZonedDateTime start;
        public final ZonedDateTime end;
        public final String code;
        public final String category;
        public final String message;

        public StatusEntryModel(StateEntry stateEntry) {
            this.start = stateEntry.start;
            this.end = stateEntry.end;
            this.code = stateEntry.code;
            this.category = stateEntry.category == null ? "" : stateEntry.category.toString();
            this.message = stateEntry.message == null ? "" : stateEntry.message.get(session.getAcceptableLanguages());
        }
    }

}
