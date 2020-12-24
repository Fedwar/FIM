package fleetmanagement.frontend.model;

import fleetmanagement.backend.vehiclecommunication.upload.filter.PathComposer;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilter;
import fleetmanagement.config.FimConfig;
import fleetmanagement.config.Settings;
import fleetmanagement.test.LicenceStub;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FilterDirectoryTest {

    @Mock
    public Settings settings;
    private File filtersRoot;

    @Before
    public void setup() {
        initMocks(this);
        filtersRoot = new File("Incoming");
        when(settings.getIncomingFolderPath()).thenReturn("");
    }

    @After
    public void after() throws IOException {
        FileUtils.deleteDirectory(filtersRoot);
    }

    @Test
    public void constructor() {
        String validPath = "ad" + File.separator + "<vehicle>" + File.separator + "<group>" + File.separator + "monitor";
        File filterDir = new File(filtersRoot, "ad");
        filterDir.mkdirs();

        UploadFilter uploadFilter = new UploadFilter("", validPath, "", "Disabled", "30");
        String cleanPath = PathComposer.getCleanPath(uploadFilter);
        FilterDirectory filterDirectory = new FilterDirectory(uploadFilter, cleanPath, new LicenceStub(), settings);

        assertNotNull(filterDirectory);
        assertEquals("ad\\", filterDirectory.fullpath);

    }

}