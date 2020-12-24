package fleetmanagement.backend.events;

import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.repositories.exception.PackageImportException;
import fleetmanagement.backend.tasks.Task;

import java.time.ZonedDateTime;
import java.util.HashMap;

public class Events {

    public static EventImpl diagnosisUpdated(Diagnosis diagnosis) {
        return new EventImpl(diagnosis, EventType.DIAGNOSIS_UPDATED);
    }

    public static EventImpl taskLogUpdated(Task task) {
        return new EventImpl(task, EventType.TASK_LOG_UPDATED);
    }

    public static EventImpl operationDataUpdated(OperationData operationData) {
        return new EventImpl(operationData, EventType.OPERATION_DATA_UPDATED);
    }

    public static EventImpl packageImportError(ZonedDateTime importStart, String fileName, PackageImportException e, String groupName) {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("packageType", e.getPackageType());
        properties.put("importStart", importStart);
        properties.put("fileName", fileName);
        properties.put("groupName", groupName);
        return new EventImpl(e, EventType.PACKAGE_IMPORT_EXCEPTION, properties);
    }

    public static EventImpl serverException(Exception ex) {
        return new EventImpl(ex, EventType.SERVER_EXCEPTION);
    }

}
