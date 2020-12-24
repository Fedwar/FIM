package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.config.FimConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@Component
public class OnDiskPackageTypeRepository implements PackageTypeRepository {

    private static final Logger logger = Logger.getLogger(OnDiskPackageTypeRepository.class);

    private final Set<PackageType> garbageCollection = new HashSet<>();
    private final Set<PackageType> autoSync = new HashSet<>();
    private String dataFile;
    private String autoSyncFile;

    @Autowired
    public OnDiskPackageTypeRepository(FimConfig config) throws IOException {
        this(config.getDataDirectory());
    }

    OnDiskPackageTypeRepository(File dataDirectory) throws IOException {
        this.dataFile = dataDirectory.getPath() + File.separator + "datatype.properties";
        this.autoSyncFile = dataDirectory.getPath() + File.separator + "autosync.properties";

        try {
            loadSettings(this.dataFile, this.garbageCollection);
        } catch (FileNotFoundException e) {
            logger.warn("Can't load package types settings. GC is disabled.");
            return;
        }

        try {
            loadSettings(this.autoSyncFile, this.autoSync);
        } catch (FileNotFoundException e) {
            logger.warn("Can't load package automatic synchronization settings. Automatic synchronization is disabled.");
        }
    }

    private void loadSettings(String filename, Set<PackageType> list) throws IOException {
        list.clear();
        Properties properties = new Properties();
        properties.load(new FileInputStream(filename));

        for (String key : properties.stringPropertyNames()) {
            list.add(PackageType.getByResourceKey(key));
        }
    }

    @Override
    public void enableGC(PackageType packageType) {
        garbageCollection.add(packageType);
    }

    @Override
    public void disableGC(PackageType packageType) {
        garbageCollection.remove(packageType);
    }

    @Override
    public void enableAutoSync(PackageType packageType) {
        autoSync.add(packageType);
    }

    @Override
    public void disableAutoSync(PackageType packageType) {
        autoSync.remove(packageType);
    }

    @Override
    public boolean isGCEnabled(PackageType packageType) {
        return garbageCollection.contains(packageType);
    }

    @Override
    public boolean isAutoSyncEnabled(PackageType packageType) {
        return autoSync.contains(packageType);
    }

    @Override
    public void disableAll() {
        garbageCollection.clear();
    }

    @Override
    public void disableAllAutoSync() {
        autoSync.clear();
    }

    private void saveSettings(String filename, Set<PackageType> list) throws IOException {
        Properties properties = new Properties();

        for (PackageType packageType: list) {
            properties.put(packageType.getResourceKey(), "1");
        }

        properties.store(new FileOutputStream(filename), null);
    }

    public void save() throws IOException {
        try {
            saveSettings(dataFile, garbageCollection);
        } catch (FileNotFoundException e) {
            logger.error("Datatypes properties file not found!", e);
            throw e;
        }

        try {
            saveSettings(autoSyncFile, autoSync);
        } catch (FileNotFoundException e) {
            logger.error("Automatic synchronization properties file not found!", e);
            throw e;
        }
    }
}
