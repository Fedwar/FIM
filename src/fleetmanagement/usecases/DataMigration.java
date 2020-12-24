package fleetmanagement.usecases;

import fleetmanagement.config.CommandsGenerator;
import fleetmanagement.config.FimConfig;
import fleetmanagement.config.Licence;
import fleetmanagement.config.LicenceImpl;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Component
public class DataMigration {
    private static final Logger logger = Logger.getLogger(DataMigration.class);
    public static final String LOGS_DIRECTORY = "logs";
    private File directory;
    @Autowired
    private FimConfig config;
    @Autowired
    private Licence licence;

    public DataMigration() {
    }

    DataMigration(File dataDirectory, Licence licence) {
        directory = dataDirectory;
        this.licence = licence;
    }

    @PostConstruct
    public void init() {
        this.directory = config.getDataDirectory();
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            logger.error("Path to import data not provided. Import cancelled.");
            return;
        }
        String sourcePath = args[0];

        File dataDir = findDataFolder(new File("").getAbsolutePath());
        LicenceImpl licence = new LicenceImpl(dataDir);

        DataMigration dataMigration = new DataMigration(dataDir, licence);
        dataMigration.importData(sourcePath);
        dataMigration.importLicence(sourcePath);
    }

    static File findDataFolder(String sourcePath) throws IOException {
        File importDirectory = new File(sourcePath);
        if (importDirectory.exists() && importDirectory.isDirectory()) {
            if (importDirectory.getName().equalsIgnoreCase("data")) {
                return importDirectory;
            } else {
                if (new File(importDirectory, "FIM-Server.exe").exists())
                    return new File(importDirectory, "App/data");
                else if (new File(importDirectory, "data").exists()) {
                    return new File(importDirectory, "data");
                }
            }
        } else {
            throw new IOException("directory not found");
        }
        throw new IOException("application data not found");
    }

    public void importData(String sourcePath) throws Exception {
        File importDirectory = findDataFolder(sourcePath);
        if (importDirectory.equals(directory)) {
            throw new IOException("Source and destination directories are the same");
        }
        clearDestinationDirectory();

        FileFilter filter = pathname -> !(pathname.getParentFile().isDirectory() && pathname.getParentFile().getName().equals(LOGS_DIRECTORY));
        FileUtils.copyDirectory(importDirectory, directory, filter, true);
        logger.info("Data imported");
    }

    void clearDestinationDirectory() throws IOException {
        Files.walk(directory.toPath())
                .filter(path -> !directory.toPath().relativize(path).startsWith(LOGS_DIRECTORY))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public void importLicence(String sourcePath) throws IOException {
        File importDirectory = findDataFolder(sourcePath);
        File commandFile = new File(importDirectory, "licence.txt");
        LicenceImpl oldLicence = new LicenceImpl(importDirectory);

        if (commandFile.exists()) {
            try {
                byte[] encoded = Files.readAllBytes(commandFile.toPath());
                String decrypt = oldLicence.decode(new String(encoded, StandardCharsets.UTF_8));
                String encrypt = CommandsGenerator.encrypt(licence.getInstallationSeed(), decrypt);
                licence.update(encrypt);
                licence.saveLicenceToFile(encrypt);
            } catch (Exception e) {
                throw new RuntimeException("Unable to import licence");
            }
            logger.info("Licence imported");
        }
    }

    public void setLicence(Licence licence) {
        this.licence = licence;
    }
}
