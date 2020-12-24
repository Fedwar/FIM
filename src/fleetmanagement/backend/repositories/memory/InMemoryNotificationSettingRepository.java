package fleetmanagement.backend.repositories.memory;

import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.repositories.disk.OnDiskNotificationSettingRepository;

import java.io.File;

public class InMemoryNotificationSettingRepository extends OnDiskNotificationSettingRepository {

    public InMemoryNotificationSettingRepository() {
        super((File)null);
    }

    @Override
    public void loadFromDisk() {
    }

    @Override
    protected File getDirectory(NotificationSetting persistable) {
        return null;
    }

    @Override
    protected void persist(NotificationSetting persistable) {
    }


}
