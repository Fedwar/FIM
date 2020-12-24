package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.NotificationSettingRepository;
import fleetmanagement.backend.notifications.settings.Type;
import fleetmanagement.backend.repositories.disk.xml.NotificationXmlFile;
import fleetmanagement.backend.repositories.disk.xml.XmlFile;
import fleetmanagement.config.FimConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OnDiskNotificationSettingRepository extends GenericOnDiskRepository<NotificationSetting, UUID> implements NotificationSettingRepository {

    private static final Logger logger = Logger.getLogger(OnDiskNotificationSettingRepository.class);

    @Autowired
    public OnDiskNotificationSettingRepository(FimConfig config) {
        super(config.getNotificationsDirectory());
    }

    public OnDiskNotificationSettingRepository(File directory) {
        super(directory);
    }

    @Override
    @PostConstruct
    public void loadFromDisk() {
        logger.debug("Loading from disk: notifications");
        super.loadFromDisk();
    }

    @Override
    protected XmlFile<NotificationSetting> getXmlFile(File directory) {
        return new NotificationXmlFile(directory);
    }

    @Override
    public List<NotificationSetting> findByType(Type type) {
        return persistables.stream().filter(x -> x.type.equals(type)).collect(Collectors.toList());
    }

    @Override
    public void insertOrReplace(NotificationSetting notificationSetting) {
        NotificationSetting persisted = tryFindById(notificationSetting.id);
        if (persisted == null) {
            insert(notificationSetting);
        } else {
            persistables.set(persistables.indexOf(persisted), notificationSetting);
            persist(notificationSetting);
        }
    }
}
