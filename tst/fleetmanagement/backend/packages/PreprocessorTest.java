package fleetmanagement.backend.packages;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.TestFiles;
import fleetmanagement.backend.packages.preprocess.PreprocessSetting;
import fleetmanagement.backend.packages.preprocess.PreprocessSettingRepository;
import fleetmanagement.backend.packages.preprocess.Preprocessor;
import fleetmanagement.backend.repositories.exception.ImportPreProcessingException;
import fleetmanagement.backend.repositories.memory.InMemoryPreprocessSettingRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;

import static fleetmanagement.backend.packages.preprocess.Preprocessor.PROCESSING_DATA;
import static org.junit.Assert.*;

public class PreprocessorTest {
    private Preprocessor tested;

    PreprocessSettingRepository settings;

    @Rule
    public TempFileRule tempDir = new TempFileRule();
    private TempFile preprocessingDir;

    @Before
    public void setup() {
        settings = new InMemoryPreprocessSettingRepository();
        preprocessingDir = tempDir.newFolder("preprocessing");
        MockitoAnnotations.initMocks(this);
        tested = new Preprocessor(settings);
    }

    @Test
    public void preprocessingMakesResult_AccordingToSettings() throws IOException {
        PreprocessSetting setting = addSetting(PackageType.DataSupply
                , "\"C:\\Program Files\\7-Zip\\7z.exe\""
                , "a -tzip <resultDir>\\package.zip .\\<dataDir>\\*"
                , null);
        String fileName = "Fahrgast-TV-Content_1.0.zip";
        File processed = tested.preprocess(setting, TestFiles.find(fileName), preprocessingDir);

        assertNotNull(processed);
        assertTrue(processed.exists());
        assertEquals("package.zip", processed.getName());
        assertEquals("result", processed.getParentFile().getName());
    }

    @Test(expected = ImportPreProcessingException.class)
    public void throwsException_WhenPreprocessingMadeNoResult() throws IOException {
        PreprocessSetting setting = addSetting(PackageType.DataSupply
                , "\"C:\\Program Files\\7-Zip\\7z.exe\""
                , null, null);
        File processed = tested.preprocess(setting, tempDir.newFile("package.zip"), preprocessingDir);
    }

    @Test(expected = ImportPreProcessingException.class)
    public void throwsException_WhenPreprocessorNotConfigured() throws IOException {
        PreprocessSetting setting = addSetting(PackageType.DataSupply, null, null, null);
        File processed = tested.preprocess(setting, tempDir.newFile("package.zip"), preprocessingDir);
    }

    @Test
    public void ifPackageFileIsNotArchive_itIsSavedToPreprocesingDir() throws Exception {
        PreprocessSetting setting = addSetting(PackageType.DataSupply
                , "xcopy <dataDir> <resultDir>\\"
                , null, null);
        File file = tempDir.newFile("preprocess.xml");
        File dataDir = new File(preprocessingDir, PROCESSING_DATA);
        File processed = tested.preprocess(setting, file, preprocessingDir);

        assertTrue(new File(dataDir, "preprocess.xml").exists());
    }

    public Preprocessor getTested() {
        return tested;
    }

// TODO preprocessing temporary disabled

//    @Test
//    public void allowsPreprocessing_WhenMaskMatches() throws IOException {
//        PreprocessSetting setting = addSetting(PackageType.DataSupply, null, null
//                , "*.zip");
//        File packageFile = tempDir.newFile("package.zip");
//
//        assertEquals(setting, tested.needPreprocessing(packageFile.getName()));
//    }

//    @Test
//    public void returnsFirstMatchingSetting() throws IOException {
//        addSetting(PackageType.DataSupply, null, null
//                , "*.xml");
//        PreprocessSetting setting = addSetting(PackageType.DataSupply, null, null
//                , "package*");
//        addSetting(PackageType.DataSupply, null, null
//                , "*.zip");
//        File packageFile = tempDir.newFile("package.zip");
//
//        assertEquals(setting, tested.needPreprocessing(packageFile.getName()));
//    }

    @Test
    public void noPreprocessing_WhenMaskDoesNotMatches() throws IOException {
        PreprocessSetting setting = addSetting(PackageType.DataSupply, null, null
                , "preprocess*");
        File packageFile = tempDir.newFile("package.zip");

        assertNull(tested.needPreprocessing(packageFile.getName()));
    }

    PreprocessSetting addSetting(PackageType packageType, String command, String options, String pattern) {
        PreprocessSetting setting = new PreprocessSetting(packageType, command, options, pattern);
        settings.insertOrReplace(setting);
        return setting;
    }

}