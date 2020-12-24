package fleetmanagement.backend.repositories.migration;

import fleetmanagement.backend.repositories.disk.DeletionHelper;
import fleetmanagement.backend.repositories.disk.WidgetXmlRepository;
import fleetmanagement.backend.widgets.Widget;

import java.io.File;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class MigrateWidgetsToUuidId {

    private final WidgetXmlRepository widgetXmlRepository;

    public MigrateWidgetsToUuidId(WidgetXmlRepository widgetXmlRepository) {
        this.widgetXmlRepository = widgetXmlRepository;
    }

    public void migrate(List<Widget> persistables, File directory) {
        List<Widget> toMigrate = persistables.stream()
                .filter(widget -> widget.id == null)
                .collect(Collectors.toList());
        toMigrate.forEach(widget -> {
            persistables.remove(widget);
            DeletionHelper.delete(new File(directory, widget.indicatorId));
        });
        toMigrate.forEach(widget -> widgetXmlRepository.insertOrReplace(
                new Widget(widget.indicatorId, widget.maxValue, widget.minValue, widget.type))
        );
    }

}
