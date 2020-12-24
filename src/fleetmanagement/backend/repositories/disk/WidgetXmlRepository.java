package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.repositories.disk.xml.WidgetXmlFile;
import fleetmanagement.backend.repositories.disk.xml.XmlFile;
import fleetmanagement.backend.repositories.migration.MigrateWidgetsToUuidId;
import fleetmanagement.backend.widgets.Widget;
import fleetmanagement.backend.widgets.WidgetRepository;
import fleetmanagement.config.FimConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;
import java.util.UUID;


@Component
public class WidgetXmlRepository extends GenericOnDiskRepository<Widget, UUID> implements WidgetRepository {

    private static final Logger logger = Logger.getLogger(WidgetXmlRepository.class);

    @Autowired
    public WidgetXmlRepository(FimConfig fimConfig) {
        super(fimConfig.getWidgetsDirectory());
    }

    public WidgetXmlRepository(File directory) {
        super(directory);
    }

    @Override
    @PostConstruct
    public void loadFromDisk() {
        logger.debug("Loading from disk: widgets");
        super.loadFromDisk();
        migrate();
    }

    private void migrate() {
        new MigrateWidgetsToUuidId(this).migrate(persistables, directory);
    }

    @Override
    protected XmlFile<Widget> getXmlFile(File dir) {
        return new WidgetXmlFile(dir);
    }

    @Override
    public void insertOrReplace(Widget widget) {
        Widget persisted = findWidgetByIndicatorId(widget.indicatorId);
        if (persisted == null) {
            insert(widget);
        } else {
            persistables.set(persistables.indexOf(persisted), widget);
            persist(widget);
        }
    }

    public Widget findWidgetByIndicatorId(String indicatorId) {
        if (indicatorId == null) {
            return null;
        }
        return persistables.stream().filter(x -> x.indicatorId.equals(indicatorId)).findFirst().orElse(null);
    }

    @Override
    public void deleteWidgetByIndicatorId(String indicatorId) {
        Widget persisted = findWidgetByIndicatorId(indicatorId);
        if (persisted != null) {
            delete(persisted.id());
        }
    }


}
