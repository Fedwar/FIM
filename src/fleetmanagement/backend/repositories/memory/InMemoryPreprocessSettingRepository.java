package fleetmanagement.backend.repositories.memory;

import fleetmanagement.backend.packages.preprocess.PreprocessSetting;
import fleetmanagement.backend.repositories.disk.PreprocessSettingXmlRepository;
import fleetmanagement.backend.repositories.disk.WidgetXmlRepository;
import fleetmanagement.backend.widgets.Widget;

import java.io.File;

public class InMemoryPreprocessSettingRepository extends PreprocessSettingXmlRepository {

    public InMemoryPreprocessSettingRepository() {
        super((File)null);
    }

    @Override
    public void loadFromDisk() {}

    @Override
    protected File getDirectory(PreprocessSetting persistable) {
        return null;
    }

    @Override
    protected void persist(PreprocessSetting object) {}

}
