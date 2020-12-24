package fleetmanagement.backend.vehiclecommunication.upload.filter;

import org.junit.Test;

import static org.junit.Assert.*;

public class PathComposerTest {


    @Test
    public void compose_removesRelativePathSymbols() {
        UploadFilter filter1 = new UploadFilter(null, ".\\folder\\name", "", "Disabled", "30");
        UploadFilter filter2 = new UploadFilter(null, "./folder/name", "", "Disabled", "30");

        assertEquals("folder\\name", PathComposer.compose(filter1, "", ""));
        assertEquals("folder/name", PathComposer.compose(filter2, "", ""));
    }

    @Test
    public void compose_removesOnlyFirstRelativePathSymbos() {
        UploadFilter filter1 = new UploadFilter(null, ".\\folder.\\name", "", "Disabled", "30");
        UploadFilter filter2 = new UploadFilter(null, "./folder./name", "", "Disabled", "30");

        assertEquals("folder.\\name", PathComposer.compose(filter1, "", ""));
        assertEquals("folder./name", PathComposer.compose(filter2, "", ""));
    }

}