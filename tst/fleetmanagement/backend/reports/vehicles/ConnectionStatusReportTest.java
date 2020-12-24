package fleetmanagement.backend.reports.vehicles;

import fleetmanagement.backend.reports.datasource.vehicles.ConnectionStatusReportDataSource;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionStatusReportTest {

    @Mock
    private ConnectionStatusReportDataSource reportDataSource;

    private ConnectionStatusReport tested;

    private File outFile;

    @Before
    public void setup() {
        ConnectionStatusReportDataSource.DataItem item = new ConnectionStatusReportDataSource.DataItem("2020-10-25");
        item.onlineCount = 2;
        when(reportDataSource.getData()).thenReturn(Collections.singletonMap("2020-10-25", item));

        tested = new ConnectionStatusReport(
                "2020-10-25",
                "2020-10-25",
                "aaa,bbb",
                "months"
        );
    }

    @Test
    public void build() throws IOException {
        tested.build(reportDataSource);

        byte[] bytes = tested.getBytes();
        assertNotNull(bytes);
        assertThat(bytes.length, Matchers.greaterThan(0));
    }

    @After
    public void tearDown() throws Exception {
        if (outFile != null) {
            FileUtils.forceDelete(outFile);
        }
    }
}