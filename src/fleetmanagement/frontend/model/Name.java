package fleetmanagement.frontend.model;

import fleetmanagement.backend.packages.*;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.tasks.TaskStatus.ClientStage;
import fleetmanagement.backend.vehicles.DiagnosticSummary;
import fleetmanagement.backend.vehicles.VehicleVersions.Versioned;
import fleetmanagement.frontend.*;

public class Name {
    public static String of(DiagnosticSummary summary, UserSession request) {
        switch (summary.type) {
            case NotSeen:
                return I18n.get(request, "err_not_seen_recently", summary.lastSeen);
            case DeviceErrors:
                return I18n.get(request, "err_device_errors", summary.brokenDevices);
            case Ok:
                return I18n.get(request, "err_diagnosis_ok");
        }

        throw new MissingLocalizationException();
    }

    public static String of(PackageType type, UserSession request) {
        switch (type) {
            case CopyStick:
                return I18n.get(request, "remote_copystick");
            case DataSupply:
                return I18n.get(request, "data_supply");
            case Indis3MultimediaContent:
                return I18n.get(request, "indis3_multimedia_content");
            case Indis5MultimediaContent:
                return I18n.get(request, "indis5_multimedia_content");
            case XccEnnoSeatReservation:
                return I18n.get(request, "xcc_enno_seat_reservation");
            case OebbDigitalContent:
                return I18n.get(request, "oebb_digital_content");
            case PassengerTvContent:
                return I18n.get(request, "passenger_tv_content");
            case SbhOutageTicker:
                return I18n.get(request, "sbh_outage_ticker");
            case ClientConfig:
                return I18n.get(request, "client_config");
        }

        throw new MissingLocalizationException();
    }

    public static String of(PackageInstallationStatus status, UserSession request) {
        switch (status.state) {
            case InstallationUpcoming:
                return I18n.get(request, "installing_x_percent", status.progressPercent);
            case Installed:
                return I18n.get(request, "installed");
            case NotInstalled:
                return I18n.get(request, "not_installed");
        }
        throw new MissingLocalizationException();
    }

    public static String of(ClientStage stage, UserSession request) {
        switch (stage) {
            case PENDING:
                return I18n.get(request, "pending");
            case INITIALIZING:
                return I18n.get(request, "task_initiated");
            case DOWNLOADING:
                return I18n.get(request, "downloading");
            case WAITING:
                return I18n.get(request, "waiting");
            case ACTIVATING:
                return I18n.get(request, "activating");
            case FINISHED:
                return I18n.get(request, "finished");
            case CANCELLED:
                return I18n.get(request, "cancelled");
            case FAILED:
                return I18n.get(request, "failed");
        }
        throw new MissingLocalizationException();
    }

    public static String of(Package pkg, UserSession request) {
        String slotDescription = pkg.slot == 0 ? "" : " (Slot " + pkg.slot + ")";
        return Name.of(pkg.type, request) + " " + pkg.version + slotDescription;
    }

    public static class MissingLocalizationException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static String of(Versioned versioned, UserSession request) {
        return Name.of(versioned.type, request);
    }

}
