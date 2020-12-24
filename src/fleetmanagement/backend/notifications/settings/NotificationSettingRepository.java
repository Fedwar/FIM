package fleetmanagement.backend.notifications.settings;

import fleetmanagement.backend.repositories.Repository;

import java.util.List;
import java.util.UUID;

public interface NotificationSettingRepository extends Repository<NotificationSetting, UUID> {

    List<NotificationSetting> findByType(Type type);

    void insertOrReplace(NotificationSetting notificationSetting);

}