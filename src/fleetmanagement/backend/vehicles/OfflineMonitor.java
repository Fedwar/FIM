package fleetmanagement.backend.vehicles;

import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.mail.MailService;
import fleetmanagement.backend.notifications.VehiclesOffline;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.NotificationSettingRepository;
import fleetmanagement.backend.notifications.settings.Type;
import gsp.util.Timers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class OfflineMonitor {
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private NotificationSettingRepository notificationSettingRepository;
    @Autowired
    private MailService mailService;
    private ScheduledExecutorService timer;
    private HashMap<NotificationSetting, NotificationStatus> status = new HashMap<>();
    public static final int DEFAULT_CHECK_INTERVAL = 20;
    static int checkIntervalSeconds = DEFAULT_CHECK_INTERVAL;

    public OfflineMonitor() {
    }

    OfflineMonitor(VehicleRepository vehicleRepository, GroupRepository groupRepository
            , NotificationSettingRepository notificationSettingRepository, MailService mailService) {
        this.vehicleRepository = vehicleRepository;
        this.groupRepository = groupRepository;
        this.notificationSettingRepository = notificationSettingRepository;
        this.mailService = mailService;
    }

    public void start(int checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
        timer = Timers.newTimer("OfflineMonitor");
        timer.scheduleWithFixedDelay(this::check, checkIntervalSeconds, checkIntervalSeconds, TimeUnit.SECONDS);
    }

    public void start() {
        timer = Timers.newTimer("OfflineMonitor");
        timer.scheduleWithFixedDelay(this::check, checkIntervalSeconds, checkIntervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        timer.shutdown();
    }

    void check() {
        List<NotificationSetting> settings = notificationSettingRepository.findByType(Type.VEHICLE_OFFLINE);
        cleanStatus(settings);
        for (NotificationSetting notificationSetting : settings) {
            VehiclesOffline notification = new VehiclesOffline(notificationSetting, vehicleRepository.listAll()
                    , groupRepository.listAll());
            if (notification.needToSend()) {
                NotificationStatus notificationStatus = status.get(notificationSetting);
                if (notificationStatus == null) {
                    send(notification);
                } else {
                    if (notificationStatus.needToSend(notification)) {
                        send(notification);
                    }
                }
            }
        }
    }

    void send(VehiclesOffline notification) {
        mailService.send(notification);
        status.put(notification.notification(), new NotificationStatus(notification.getOfflineVehicles()));
    }

    synchronized void cleanStatus(List<NotificationSetting> settings) {
        List<NotificationSetting> collect = status.keySet().stream()
                .filter(s -> !settings.contains(s))
                .collect(Collectors.toList());
        for (NotificationSetting notificationSetting : collect) {
            status.remove(notificationSetting.id);
        }
    }

    public class NotificationStatus {
        public Set<UUID> offlineVehicles;
        public LocalDateTime sent;

        public NotificationStatus(List<Vehicle> offlineVehicles) {
            this.offlineVehicles = offlineVehicles.stream().map(vehicle -> vehicle.id).collect(Collectors.toSet());
            sent = LocalDateTime.now();
        }

        boolean needToSend(VehiclesOffline notification) {
            Set<UUID> collect = notification.getOfflineVehicles().stream()
                    .map(vehicle -> vehicle.id)
                    .collect(Collectors.toSet());
            return !offlineVehicles.equals(collect);
        }

    }
}
