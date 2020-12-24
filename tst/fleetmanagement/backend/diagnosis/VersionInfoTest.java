package fleetmanagement.backend.diagnosis;

import org.junit.Test;

import static org.junit.Assert.*;

public class VersionInfoTest {

    @Test
    public void cloneMethodTest() {

        VersionInfo versionInfo = new VersionInfo("soft1", "font1");

        VersionInfo clone = versionInfo.clone();
        assertEquals("soft1", clone.get(VersionInfo.VersionType.Software));
        assertEquals("font1", clone.get(VersionInfo.VersionType.Fontware));

    }

}