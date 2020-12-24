package fleetmanagement.backend.reports.vehicles;

import fleetmanagement.backend.reports.datasource.vehicles.OperationDataReportDataSource;
import fleetmanagement.backend.repositories.disk.OperationDataHistorySQLiteRepository;
import fleetmanagement.backend.repositories.memory.InMemoryOperationDataRepository;
import fleetmanagement.test.TestFolderManager;
import fleetmanagement.test.TestScenarioPrefilled;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OperationDataReportTest {
    private TestScenarioPrefilled scenario;
    private OperationDataReport tested;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    @Rule
    public TestFolderManager reportFolder = new TestFolderManager("reports");
    private File repositoryDir;

    @Before
    public void setup() {
        scenario = new TestScenarioPrefilled();
        //importPackage = mock(ImportPackage.class);

        tested = new OperationDataReport(
                "2018-01-31",
                "2018-03-01",
                scenario.vehicle1.id.toString() + "," + scenario.vehicle2.id.toString(),
                "months",
                "id11,id12,id21,id22"
        );

        repositoryDir = tempFolder.getRoot();
    }

    @Test
    public void build() throws IOException {
        Map<String, String> monthsReportFilters = new HashMap<>();
        monthsReportFilters.put("earliestReportDate", "2019-03-22");
        monthsReportFilters.put("latestReportDate", "2020-12-01");
        monthsReportFilters.put(
                "selectedVehicles",
                scenario.vehicle1.id.toString() + "," + scenario.vehicle2.id.toString()
        );
        monthsReportFilters.put("selectedIndicators", "id11,id12,id21,id22");
        monthsReportFilters.put("rangeBy", "months");

        InMemoryOperationDataRepository inMemoryOperationDataRepository =
                new InMemoryOperationDataRepository(new OperationDataHistorySQLiteRepository(repositoryDir));
        tempFolder.newFolder(scenario.vehicle1.id.toString());
        tempFolder.newFolder(scenario.vehicle2.id.toString());

        OperationDataReportDataSource operationDataReportDataSource = new OperationDataReportDataSource(
                inMemoryOperationDataRepository,
                scenario.vehicleRepository,
                monthsReportFilters
        );
        tested.build(operationDataReportDataSource);

        byte[] bytes = tested.getBytes();
        assertNotNull(bytes);
        assertThat(bytes.length, Matchers.greaterThan(0));
    }
}