package fleetmanagement.frontend.model;

import fleetmanagement.backend.tasks.LogEntry;
import fleetmanagement.backend.vehicles.Vehicle;
import lombok.Getter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class LogMessage {
    @Getter
    private Instant instant;
    public String dateTime;
    public String vehicleId;
    public String vehicleName;
    public String severityText;
    public String severityCss;
    public String message;

    public LogMessage(Instant instant, String dateTime, String vehicleId, String vehicleName, String severityText,
                      String severityCss, String message) {
        this.instant = instant;
        this.dateTime = dateTime;
        this.vehicleId = vehicleId;
        this.vehicleName = vehicleName;
        this.severityText = severityText;
        this.severityCss = severityCss;
        this.message = message;
    }

    public static LogMessage create(LogEntry logEntry, Vehicle v, DateTimeFormatter timeFormatter) {
        return create(logEntry,
                v == null ? null : v.id,
                v == null ? null : v.getName(),
                timeFormatter);
    }

    private static LogMessage create(LogEntry logEntry, UUID vehicleId, String vehicleName, DateTimeFormatter timeFormatter) {
        String severityText = capitalizeFirstLetter(logEntry.severity.toString().toLowerCase());
        String severityCss = toBootstrapCss(logEntry.severity);
        String timeStamp = timeFormatter.format(logEntry.time);
        return new LogMessage(logEntry.time, timeStamp,
                vehicleId == null ? null : vehicleId.toString(), vehicleName, severityText, severityCss, logEntry.message);
    }

    private static String toBootstrapCss(LogEntry.Severity severity) {
        switch (severity) {
            case ERROR:
                return "danger";
            case WARNING:
                return "warning";
            case INFO:
                return "";
        }

        return "";
    }

    private static String capitalizeFirstLetter(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

}
