package fleetmanagement.usecases;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.packages.ActivityLog;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.importers.PackageImporter;
import fleetmanagement.backend.packages.preprocess.PreprocessSetting;
import fleetmanagement.backend.packages.preprocess.Preprocessor;
import fleetmanagement.backend.repositories.exception.PackageImportException;
import fleetmanagement.backend.repositories.exception.PackageTypeNotLicenced;
import fleetmanagement.config.FimConfig;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.TempDirectory;
import gsp.zip.CaseInsensitiveFileSystem;
import gsp.zip.RealFilesystem;
import gsp.zip.Zip;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

//todo different instances originally for frontend and backend
@Component
public class ImportPackage {

    private static final Logger logger = Logger.getLogger(ImportPackage.class);

    @Autowired
    private FimConfig config;
    @Autowired
    private PackageRepository packages;
    private List<PackageImporter> allImporters = new ArrayList<>();
    private TempDirectory tempDir;
    @Autowired
    private Preprocessor preprocessor;
    private final Zip zip = new Zip(new CaseInsensitiveFileSystem(new RealFilesystem()));
    @Autowired
    private Licence licence;

    @SuppressWarnings("serial")
    public static class UnknownPackageType extends PackageImportException {
        public UnknownPackageType(String filename) {
            super(null, "package_import.unknownPackageType", filename);
        }
    }

    @SuppressWarnings("serial")
    public static class WrongPackageType extends PackageImportException {
        public WrongPackageType(PackageType packageType, String filename) {
            super(null, "package_import.wrongPackageType", filename, packageType.name());
        }
    }

    @SuppressWarnings("serial")
    public static class PackageExistsException extends PackageImportException {
        public PackageExistsException(Package pkg) {
            super(pkg.type,
                    pkg.slot == 0 ? "package_import.packageExistsTypeVersion" : "package_import.packageExistsTypeVersionSlot",
                    pkg.type, pkg.version, pkg.slot);
        }
    }

    @SuppressWarnings("serial")
    public static class FileNotFound extends Exception {
        public FileNotFound(File file) {
            super("Imported file does not exists: " + file.getAbsolutePath());
        }
    }

    @SuppressWarnings("serial")
    public static class UnknownGroup extends Exception {
        public UnknownGroup(File file) {
            super("Unknown import group. Package file: " + file.getAbsolutePath());
        }
    }

    @SuppressWarnings("serial")
    public static class FileIsBlocked extends Exception {
        public FileIsBlocked(File file) {
            super("File '" + file.getAbsolutePath() + "' is blocked. Waiting");
        }
    }

    public ImportPackage() {
    }

    @PostConstruct
    public void init() {
        if (tempDir == null) {
            tempDir = new TempDirectory(config.getDataDirectory());
        }
        for (PackageType type : PackageType.values()) {
            this.allImporters.add(type.getPackageImporter());
        }
    }

    public ImportPackage(PackageRepository packages, TempDirectory tempDir, Licence licence, Preprocessor preprocessor) {
        this.tempDir = tempDir;
        this.packages = packages;
        this.licence = licence;
        this.preprocessor = preprocessor;
        init();
    }

    public Package importPackage(String filename, InputStream data, String source, String triggered_by) throws IOException {
        return importPackage(filename, data, source, null, triggered_by);
    }

    public Package importPackage(String filename, InputStream data, String source, Group group, String triggered_by) throws IOException {
        logger.info("Importing new package: " + filename);
        File importDirectory = createTempDirectory("PackageImport_" + filename);
        File preprocessDirectory = null;
        PackageType packageType = null;
        File uploaded = null;
        try {
            uploaded = tempDir.getPath(filename);
            FileUtils.copyInputStreamToFile(data, uploaded);
            data = new FileInputStream(uploaded);
            PreprocessSetting setting = preprocessor.needPreprocessing(filename);
            if (setting != null) {
                preprocessDirectory = createTempDirectory("Preprocess_" + filename);
                File processed = preprocessor.preprocess(setting, filename, data, preprocessDirectory);
                filename = processed.getName();
                data = new FileInputStream(processed);
                packageType = setting.packageType;
            }

            zip.unpack(data, importDirectory);
            return importPackage(uploaded, filename, importDirectory, source, group, packageType, triggered_by);
        } finally {
            FileUtils.deleteQuietly(preprocessDirectory);
            FileUtils.deleteQuietly(importDirectory);
            FileUtils.deleteQuietly(uploaded);
        }
    }

    private File createTempDirectory(String relativePath) {
        File createdDir = tempDir.getPath(relativePath);
        createdDir.mkdirs();
        return createdDir;
    }

    private Package importPackage(File archive, String filename, File importDirectory, String source, Group group, PackageType packageType, String triggered_by) {
        List<PackageImporter> importers = allImporters;
        if (packageType != null) {
            importers = allImporters.stream()
                    .filter(i -> i.getPackageType().equals(packageType))
                    .collect(Collectors.toList());
        }
        for (PackageImporter importer : importers) {
            if (importer.canImportPackage(filename, importDirectory)) {
                if (!licence.isPackageTypeAvailable(importer.getPackageType())) {
                    ActivityLog.packageMessage(ActivityLog.Operations.NO_LICENCE, null, filename, null, triggered_by);
                    logger.info("Can't import package because it is not allowed by the licence!");
                    throw new PackageTypeNotLicenced(importer.getPackageType());
                }
                try {
                    Package pkg = importer.importPackage(filename, importDirectory);
                    pkg.source = source;
                    if (group != null)
                        pkg.groupId = group.id;
                    pkg.archive = archive;

                    checkForDuplicates(pkg);
                    packages.insert(pkg);

                    return pkg;
                } catch (IOException e) {
                    logger.error("Unable to import package: " + filename, e);
                    throw new PackageImportException(importer.getPackageType(), e.toString());
                }
            }
        }

        if (packageType == null) {
            throw new UnknownPackageType(filename);
        } else {
            throw new WrongPackageType(packageType, filename);
        }

    }

    private void checkForDuplicates(Package pkg) {
        List<Package> duplicates = packages.getDuplicates(pkg);
        boolean exists = duplicates.stream().anyMatch(p -> p != pkg && Objects.equals(p.groupId, pkg.groupId));
        if (exists) {
            throw new PackageExistsException(pkg);
        }
    }

    void setAllImporters(List<PackageImporter> allImporters) {
        this.allImporters = allImporters;
    }
}
