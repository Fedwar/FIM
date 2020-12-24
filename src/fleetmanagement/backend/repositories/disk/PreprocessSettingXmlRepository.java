package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.packages.preprocess.PreprocessSetting;
import fleetmanagement.backend.packages.preprocess.PreprocessSettingRepository;
import fleetmanagement.backend.repositories.disk.xml.PreprocessSettingXmlFile;
import fleetmanagement.backend.repositories.disk.xml.XmlFile;
import fleetmanagement.config.FimConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.UUID;

@Component
public class PreprocessSettingXmlRepository extends GenericOnDiskRepository<PreprocessSetting, UUID>
        implements PreprocessSettingRepository {

    private static final Logger logger = Logger.getLogger(PreprocessSettingXmlRepository.class);

    @Autowired
    public PreprocessSettingXmlRepository(FimConfig config) {
        super(config.getDataDirectory());
    }

    public PreprocessSettingXmlRepository(File directory) {
        super(directory);
    }

    @Override
    @PostConstruct
    public void loadFromDisk() {
        logger.debug("Loading from disk: preprocess setting xml");
        super.loadFromDisk();
    }

    @Override
    protected XmlFile<PreprocessSetting> getXmlFile(File directory) {
        return new PreprocessSettingXmlFile(directory);
    }

    @Override
    public void insertOrReplace(PreprocessSetting object) {
        PreprocessSetting persisted = tryFindById(object.id());
        if (persisted == null) {
            insert(object);
        } else {
            persistables.set(persistables.indexOf(persisted), object);
            persist(object);
        }
    }
}
