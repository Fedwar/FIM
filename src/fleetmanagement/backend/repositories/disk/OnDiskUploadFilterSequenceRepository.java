package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.repositories.disk.xml.UploadFilterSequenceXmlFile;
import fleetmanagement.backend.repositories.disk.xml.XmlFile;
import fleetmanagement.backend.settings.Setting;
import fleetmanagement.backend.vehiclecommunication.upload.filter.FilterSequenceRepository;
import fleetmanagement.backend.vehiclecommunication.upload.filter.FilterType;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilter;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilterSequence;
import fleetmanagement.config.FimConfig;
import fleetmanagement.config.Settings;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.UUID;
import java.util.function.Consumer;

@Component
public class OnDiskUploadFilterSequenceRepository extends GenericOnDiskRepository<UploadFilterSequence, UUID> implements FilterSequenceRepository {

    private static final Logger logger = Logger.getLogger(OnDiskUploadFilterSequenceRepository.class);

    @Autowired
    private Settings settings;

    @Autowired
    public OnDiskUploadFilterSequenceRepository(FimConfig config) {
        super(config.getUploadFiltersDirectory());
    }

    public OnDiskUploadFilterSequenceRepository(File directory, Settings settings) {
        super(directory);
        this.settings = settings;
    }

    @Override
    public void createFilterDirectories() {
        persistables.stream().flatMap(s -> s.filters.stream()).forEach(this::createFilterDirectory);
    }

    public File getFiltersRootDirectory() {
        String incomingFolderPath = settings.getIncomingFolderPath();
        return new File(incomingFolderPath);
    }

    private void createFilterDirectory(UploadFilter filter) {
        File filtersRootDirectory = getFiltersRootDirectory();
        File filterPath = filter.getAbsoluteCleanPath(filtersRootDirectory);
        if (!filterPath.exists() && !filterPath.mkdirs())
            logger.warn("Can not create filter directory " + filterPath.getAbsolutePath());

    }

    @PostConstruct
    @Override
    public void loadFromDisk() {
        logger.debug("Loading from disk: upload filter sequence");
        super.loadFromDisk();
        createFilterDirectories();
    }

    @Override
    protected XmlFile<UploadFilterSequence> getXmlFile(File directory) {
        return new UploadFilterSequenceXmlFile(directory);
    }

    @Override
    protected void persist(UploadFilterSequence sequence) {
        super.persist(sequence);
        sequence.filters.forEach(this::createFilterDirectory);
    }

    @Override
    public void updateOrInsert(Consumer<UploadFilterSequence> updateConsumer) {
        UploadFilterSequence filterSequence = findByType(FilterType.AD_FILTER_TYPE);
        if (filterSequence == null) {
            filterSequence = new UploadFilterSequence(FilterType.AD_FILTER_TYPE);
            updateConsumer.accept(filterSequence);
            insert(filterSequence);
        } else {
            update(filterSequence.id, updateConsumer);
        }
    }

    @Override
    public UploadFilterSequence findByType(FilterType type) {
        UploadFilterSequence filterSequence = persistables.stream().filter(x -> x.type.equals(type)).findFirst().orElse(null);
        if (filterSequence == null) {
            filterSequence = new UploadFilterSequence(type);
            insert(filterSequence);
        }
        return filterSequence;
    }
}
