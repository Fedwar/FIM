package fleetmanagement.backend.vehiclecommunication.upload.filter;

import gsp.testutil.TemporaryDirectory;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;

import static fleetmanagement.backend.vehiclecommunication.upload.filter.FilterType.AD_FILTER_TYPE;
import static org.junit.Assert.*;

public class UploadFilterSequenceTest {

    UploadFilterSequence tested;

    TemporaryDirectory tempDir;

    @Before
    public void setup()  {
        tempDir = TemporaryDirectory.create();
        tested = new UploadFilterSequence(AD_FILTER_TYPE);
    }

    @Test
    public void addFilter_acceptPathWithTags() {
        String validPath = "info" + File.separator + "<vehicle>" + File.separator + "<group>" + File.separator + "monitor";
        tested.addFilter(new UploadFilter("", validPath, "", "Disabled", "30"));

        assertEquals(1, tested.filters.size());
    }

    @Test
    public void addFilter_acceptPathThatBeginsFromTags() {
        String validPath = "<vehicle>" + File.separator + "<group>" + File.separator + "monitor";
        tested.addFilter(new UploadFilter("", validPath, "", "Disabled", "30"));

        assertEquals(1, tested.filters.size());

    }

}