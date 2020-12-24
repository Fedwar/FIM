package fleetmanagement.backend.repositories.memory;

import fleetmanagement.backend.accounts.Account;
import fleetmanagement.backend.repositories.disk.AccountXmlRepository;
import fleetmanagement.backend.repositories.disk.WidgetXmlRepository;
import fleetmanagement.backend.widgets.Widget;

import java.io.File;

public class InMemoryWidgetRepository extends WidgetXmlRepository {

    public InMemoryWidgetRepository() {
        super((File)null);
    }

    @Override
    public void loadFromDisk() {}

    @Override
    protected File getDirectory(Widget persistable) {
        return null;
    }

    @Override
    protected void persist(Widget object) {}

}
