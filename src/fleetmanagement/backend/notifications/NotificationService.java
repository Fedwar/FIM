package fleetmanagement.backend.notifications;

import fleetmanagement.backend.events.Event;
import fleetmanagement.backend.events.EventType;
import fleetmanagement.backend.mail.MailService;
import fleetmanagement.backend.mail.MailServiceImpl;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.NotificationSettingRepository;
import fleetmanagement.backend.notifications.settings.Type;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.frontend.model.Logs;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

import static fleetmanagement.backend.notifications.settings.Type.*;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Component
public class NotificationService {

    private static final Logger logger = Logger.getLogger(NotificationService.class);
    private final Map<EventType, List<Type>> eventMap;
    @Autowired
    private NotificationSettingRepository notificationSettingRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private MailService mailService;

    public NotificationService() {
        eventMap = new HashMap<>();
        eventMap.put(EventType.SERVER_EXCEPTION, Arrays.asList(new Type[]{SERVER_EXCEPTION}));
        eventMap.put(EventType.TASK_LOG_UPDATED, Arrays.asList(new Type[]{PACKAGE_INSTALL_ERROR}));
        eventMap.put(EventType.DIAGNOSIS_UPDATED, Arrays.asList(new Type[]{DIAGNOSIS_MAX_ERRORS, DIAGNOSED_DEVICE_ERROR}));
        eventMap.put(EventType.OPERATION_DATA_UPDATED, Arrays.asList(new Type[]{INDICATOR_VALUE_RANGE, INDICATOR_INVALID_VALUE}));
        eventMap.put(EventType.PACKAGE_IMPORT_EXCEPTION, Arrays.asList(new Type[]{PACKAGE_IMPORT_ERROR}));
    }

    NotificationService(NotificationSettingRepository notificationSettingRepository
            , VehicleRepository vehicleRepository, TaskRepository taskRepository
            , MailService mailService) {
        this();
        this.notificationSettingRepository = notificationSettingRepository;
        this.vehicleRepository = vehicleRepository;
        this.taskRepository = taskRepository;
        this.mailService = mailService;
    }

    public List<Type> getAssignedNotificationTypes(EventType eventType) {
        return eventMap.get(eventType);
    }

    public void processEvent(Event event) {
        for (NotificationSetting notificationSetting : getEventNotifications(event)) {
            Notification notification = buildNotification(notificationSetting, event);
            if (notification.needToSend()) {
                mailService.send(notification);
            }
        }
    }

    private List<Type> eventNotificationTypes(Event event) {
        return emptyIfNull(eventMap.get(event.getType()));
    }

    private List<NotificationSetting> getEventNotifications(Event event) {
        return eventNotificationTypes(event).stream()
                .flatMap(type -> emptyIfNull(notificationSettingRepository.findByType(type)).stream())
                .collect(Collectors.toList());
    }

    private Notification buildNotification(NotificationSetting notificationSetting, Event event) {
        switch (notificationSetting.type) {
            case DIAGNOSIS_MAX_ERRORS:
                return new DiagnosisMaxErrors(notificationSetting, vehicleRepository, event);
            case DIAGNOSED_DEVICE_ERROR:
                return new DiagnosedDeviceError(notificationSetting, vehicleRepository, event);
            case INDICATOR_INVALID_VALUE:
                return new IndicatorInvalidValue(notificationSetting, vehicleRepository, event);
            case INDICATOR_VALUE_RANGE:
                return new IndicatorValueRange(notificationSetting, vehicleRepository, event);
            case PACKAGE_INSTALL_ERROR:
                return new PackageInstallError(notificationSetting, vehicleRepository, event);
            case SERVER_EXCEPTION:
                return new ServerException(notificationSetting, new Logs(vehicleRepository, taskRepository), event);
            case VEHICLE_OFFLINE:
                return new ServerException(notificationSetting, new Logs(vehicleRepository, taskRepository), event);
            case PACKAGE_IMPORT_ERROR:
                return new PackageImportError(notificationSetting, event);
        }
        return null;
    }

}
