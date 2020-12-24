package fleetmanagement.backend.repositories.disk.xml;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.TestFiles;
import fleetmanagement.backend.diagnosis.*;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import static org.junit.Assert.*;

public class DiagnosisXmlFileTest {

    @Rule
    public TempFileRule tempFolder = new TempFileRule();

    @Test
    public void loadsLegacyXmlFile() throws IOException {
        TempFile file = tempFolder.newFolder(UUID.randomUUID().toString()).append("diagnosis.xml");
        FileUtils.copyFile(TestFiles.find("legacy-database-files/diagnosis.xml"), file);
        DiagnosisXmlFile tested = new DiagnosisXmlFile(file.getParentFile());
        Diagnosis diagnosis = tested.load();

        DiagnosedDevice device = diagnosis.getDevice("0x10211");
        assertNotNull(device);

        ErrorHistory errorHistory = device.getErrorHistory();
        StateEntry currentState = device.getCurrentState().get(0);

        assertEquals("FT95-1 RBB", device.getName().get(Locale.getDefault()));
        assertEquals("FT95-1 RB", device.getType());
        assertEquals("ST952A02.0", device.getVersion(VersionInfo.VersionType.Software));
        assertEquals("FT45.22", device.getVersion(VersionInfo.VersionType.Fontware));
        assertEquals(null, device.getStatus());
        assertEquals(false, device.isDisabled());
        assertEquals(ErrorCategory.OK, currentState.category);
        assertEquals(2, errorHistory.getEntries().size());
    }

    @Test
    public void savesVersionsInfoCorrectly() {
        HashMap<String, String> versionsMap = new HashMap<>();
        versionsMap.put(VersionInfo.VersionType.Fontware.toString(), "1.0");
        versionsMap.put(VersionInfo.VersionType.Software.toString(), "1.2");
        versionsMap.put("OS", "2.1");
        versionsMap.put("Hardware", "3.2");

        DiagnosedDevice device = new DiagnosedDevice("id", "location", new LocalizedString("name"), "type", null
                , null, true, new VersionInfo(versionsMap), null);
        Diagnosis diagnosis = new Diagnosis(UUID.randomUUID(), ZonedDateTime.now(), Collections.singletonList(device));
        Diagnosis loaded = saveLoadDiagnosis(diagnosis);

        assertEquals(loaded.getDevice("id").getVersionsInfo().getAll(), versionsMap);
    }

    private Diagnosis saveLoadDiagnosis(Diagnosis diagnosis) {
        TempFile uuidFolder = tempFolder.newFolder(UUID.randomUUID().toString());
        DiagnosisXmlFile diagnosisXmlFile = new DiagnosisXmlFile(uuidFolder);
        diagnosisXmlFile.save(diagnosis);
        return diagnosisXmlFile.load();
    }

}