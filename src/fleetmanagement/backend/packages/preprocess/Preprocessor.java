package fleetmanagement.backend.packages.preprocess;

import fleetmanagement.backend.repositories.exception.ImportPreProcessingException;
import fleetmanagement.backend.settings.SettingName;
import gsp.exec.CommandLineParser;
import gsp.exec.ProcessStartException;
import gsp.zip.CaseInsensitiveFileSystem;
import gsp.zip.RealFilesystem;
import gsp.zip.Zip;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class Preprocessor {

    private static final Logger logger = Logger.getLogger(Preprocessor.class);
    public static final String PROCESSING_RESULT = "result";
    public static final String PROCESSING_DATA = "data";
    @Autowired
    private PreprocessSettingRepository settings;
    private final Zip zip = new Zip(new CaseInsensitiveFileSystem(new RealFilesystem()));

    public Preprocessor() {
    }

    public Preprocessor(PreprocessSettingRepository settingsRepository) {
        this.settings = settingsRepository;
    }

    public File preprocess(PreprocessSetting setting, File importFile, File preprocessDir) throws ImportPreProcessingException, IOException {
        return preprocess(setting, importFile.getName(), new FileInputStream(importFile), preprocessDir);
    }

    private void prepare(String fileName, InputStream data, File preprocessDir) throws IOException {
        File dataDir = new File(preprocessDir, PROCESSING_DATA);
        if (fileName.endsWith(".zip"))
            zip.unpack(data, dataDir);
        else
            FileUtils.copyInputStreamToFile(data, new File(dataDir, fileName));
    }

    String buildCommand(PreprocessSetting setting) {
        if (setting.command == null)
            return "";

        String command = setting.command;
        if (setting.options != null)
            command += " " + setting.options;
        command = command.replace("<resultDir>", PROCESSING_RESULT);
        command = command.replace("<dataDir>", PROCESSING_DATA);

        return command;
    }

    public File preprocess(PreprocessSetting setting, String fileName, InputStream data, File preprocessDir) throws ImportPreProcessingException, IOException {
        logger.info("Preprocessing of file " + fileName + " started");
        prepare(fileName, data, preprocessDir);
        String command = buildCommand(setting);
        if (command.isEmpty())
            throw new ImportPreProcessingException("Preprocessing tool is not configured");

        try {
            Process process = start(command, preprocessDir, null);
            int i = process.waitFor();

            if (i != 0)
                throw new InterruptedException();

            File processed = new File(preprocessDir, PROCESSING_RESULT);
            if (processed.exists() && processed.listFiles().length > 0) {
                return processed.listFiles()[0];
            } else {
                throw new ImportPreProcessingException("Preprocessing of package " + fileName + " does not create any result");
            }
        } catch (Exception e) {
            throw new ImportPreProcessingException(fileName, e);
        }
    }

    // TODO preprocessing temporary disabled
    public PreprocessSetting needPreprocessing(String packageName) {
//        for (PreprocessSetting setting : settings.listAll()) {
//            String mask = setting.fileNamePattern;
//            if (mask == null || mask.isEmpty())
//                continue;
//            String regex = null;
//            if (mask.indexOf("*") >= 0) {
//                regex = mask.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*");
//            }
//            boolean matches = false;
//            if (regex == null)
//                matches = packageName.equals(mask);
//            else
//                matches = packageName.matches(regex);
//            if (matches)
//                return setting;
//        }
        return null;

    }

    private Process start(String command, File workingDirectory, String[] environmentVariables) {
        String[] token = new CommandLineParser().parse(command);
        try {
            return Runtime.getRuntime().exec(token, environmentVariables, workingDirectory);
        } catch (IOException e) {
            logger.error("Error executing process: " + command, e);
            throw new ProcessStartException(command, workingDirectory, e);
        }
    }
}
