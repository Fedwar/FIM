package fleetmanagement.usecases;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.TestFiles;
import fleetmanagement.backend.diagnosis.DiagnosisHistoryRepository;
import fleetmanagement.backend.operationData.OperationDataHistoryRepository;
import fleetmanagement.backend.repositories.disk.DiagnosisHistorySQLiteRepository;
import fleetmanagement.backend.repositories.disk.OperationDataHistorySQLiteRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.config.Settings;
import fleetmanagement.test.TestScenarioPrefilled;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class StorageManagerTest {

    private DiagnosisHistoryRepository diagnosisHistory;
    private OperationDataHistoryRepository operationDataHistory;

    private StorageManager tested;
    private TestScenarioPrefilled scenario;

    @Rule
    public TempFileRule tempFolder = new TempFileRule();
    @Mock
    Settings settings;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        scenario = new TestScenarioPrefilled();

        for (Vehicle vehicle : scenario.vehicleRepository.listAll()) {
            TempFile vehicleDir = tempFolder.newFolder(vehicle.id.toString());
            FileUtils.copyDirectory(TestFiles.find("storageManager"), vehicleDir);
        }

        diagnosisHistory = new DiagnosisHistorySQLiteRepository(tempFolder);
        operationDataHistory = new OperationDataHistorySQLiteRepository(tempFolder);
        tested = new StorageManager(diagnosisHistory, operationDataHistory,
                scenario.vehicleRepository, scenario.licence);
        tested.setSettings(settings);
    }

    long getFilesSize(String fileName) {
        IOFileFilter fileFilter = nameFileFilter(fileName);
        Collection<File> files = FileUtils.listFiles(tempFolder, fileFilter, TrueFileFilter.INSTANCE);
        return files.stream()
                .map(File::length)
                .collect(Collectors.summarizingLong(Long::longValue))
                .getSum();
    }

    @Test
    public void operationalDataSizeReduced() {
        String dbFileName = OperationDataHistorySQLiteRepository.DB_NAME;
        long limit = getFilesSize(dbFileName) - 100;
        when(settings.getOperationalDataLimit()).thenReturn(limit / 1073741824.0);

        tested.reduce();

        assertTrue(getFilesSize(dbFileName) < limit);
    }

    @Test
    public void diagnosisDataSizeReduced() {
        String dbFileName = DiagnosisHistorySQLiteRepository.DB_NAME;
        long limit = getFilesSize(dbFileName) - 100;
        when(settings.getDiagnosisDataLimit()).thenReturn(limit / 1073741824.0);

        tested.reduce();

        assertTrue(getFilesSize(dbFileName) < limit);
    }

    @Test
    public void doesNotReduce_IfLimitNotSet() {
        String dbFileName = DiagnosisHistorySQLiteRepository.DB_NAME;
        double filesSize = getFilesSize(dbFileName);
        tested.reduce();

        assertTrue(getFilesSize(dbFileName) == filesSize);
    }

}