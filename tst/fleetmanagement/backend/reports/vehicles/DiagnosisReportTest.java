package fleetmanagement.backend.reports.vehicles;

import fleetmanagement.backend.reports.datasource.vehicles.DiagnosisReportDataSource;
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

public class DiagnosisReportTest {
    private TestScenarioPrefilled scenario;
    private DiagnosisReport tested;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    @Rule
    public TestFolderManager reportFolder = new TestFolderManager("reports");

    @Before
    public void setup() {
        scenario = new TestScenarioPrefilled();
        //importPackage = mock(ImportPackage.class);

        tested = new DiagnosisReport(
                "2018-01-31",
                "2018-03-01",
                scenario.vehicle1.id.toString() + "," + scenario.vehicle2.id.toString(),
                "months"
        );
    }

    @Test
    public void build() throws IOException {
        Map<String, String> monthsReportFilters = new HashMap<>();
        monthsReportFilters.put("earliestReportDate", "2018-01-31");
        monthsReportFilters.put("latestReportDate", "2018-03-01");
        monthsReportFilters.put(
                "selectedVehicles",
                scenario.vehicle1.id.toString() + "," + scenario.vehicle2.id.toString() +
                        "," + scenario.vehicle3.id.toString()
        );
        monthsReportFilters.put("rangeBy", "months");

        DiagnosisReportDataSource diagnosisReportDataSource = new DiagnosisReportDataSource(
                scenario.diagnosisRepository,
                scenario.vehicleRepository,
                monthsReportFilters
        );
        tested.build(diagnosisReportDataSource);

        byte[] bytes = tested.getBytes();
        assertNotNull(bytes);
        assertThat(bytes.length, Matchers.greaterThan(0));
    }
}