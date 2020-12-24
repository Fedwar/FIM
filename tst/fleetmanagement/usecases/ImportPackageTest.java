package fleetmanagement.usecases;

import fleetmanagement.TestFiles;
import fleetmanagement.TestObjectFactory;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.preprocess.PreprocessSetting;
import fleetmanagement.backend.packages.preprocess.Preprocessor;
import fleetmanagement.backend.repositories.exception.PackageImportException;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.TempDirectory;
import fleetmanagement.usecases.ImportPackage.UnknownPackageType;
import gsp.testutil.TemporaryDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ImportPackageTest {
    private static final String IMPORT_SOURCE = "Source: Unit test";

    @Mock
    private PackageRepository packageRepository;
    @Mock
    private Preprocessor preprocessor;
    @Mock
    private Licence licence;

    private ImportPackage tested;
    private TemporaryDirectory dataDir = TemporaryDirectory.create();

    @Before
    public void setup() {
        when(licence.isPackageTypeAvailable(any())).thenReturn(true);
        tested = new ImportPackage(packageRepository, new TempDirectory(dataDir), licence, preprocessor);
    }

    @After
    public void teardown() {
        dataDir.delete();
    }

    @Test
    public void importsDataSupply() throws Exception {
        Package pkg = importPackage("Nearly_Empty_DV.zip");

        assertEquals(PackageType.DataSupply, pkg.type);
        assertEquals("V2015.04", pkg.version);
        assertEquals(2, pkg.size.files);
        assertEquals(12980, pkg.size.bytes);
        assertEquals(1, (int) pkg.slot);
        assertEquals(IMPORT_SOURCE, pkg.source);
    }

    @Test(expected = PackageImportException.class)
    public void importChecksForDuplicates() throws Exception {
        Package existing = TestObjectFactory.createPackage(PackageType.DataSupply, "V2015.04", 1, "", "");
        when(packageRepository.getDuplicates(any())).thenReturn(Collections.singletonList(existing));

        Package pkg = importPackage("Nearly_Empty_DV.zip");
    }

    @Test
    public void deletesCopyStickFilesWhenImportingDataSupply() throws Exception {
        Package pkg = importPackage("Nearly_Empty_DV_with_copystick.ini.zip");

        assertFalse(new File(pkg.path, "copystick.ini").exists());
        assertFalse(new File(pkg.path, "copycard.ini").exists());
    }

    @Test
    public void recognizesDataSupplySlot() throws Exception {
        Package pkg = importPackage("Nearly_Empty_DV_Slot2.zip");

        assertEquals(2, (int) pkg.slot);
    }

    @Test
    public void removesEmptySlotsDuringImport() throws Exception {
        Package pkg = importPackage("Nearly_Empty_DV_Slot1_With_Empty_IBIS2.zip");

        assertFalse(new File(pkg.path, "IBIS2").exists());
        assertEquals(1, (int) pkg.slot);
    }

    @Test(expected = PackageImportException.class)
    public void throwsExceptionForDataSupplyWithMoreThan1Slot() throws Exception {
        importPackage("DV_with_Two_Slots.zip");
    }

    @Test(expected = UnknownPackageType.class)
    public void recognizesInvalidUpdatePackages() throws Exception {
        importPackage("test.zip");
    }

    @Test
    public void importsCopyStick() throws Exception {
        Package pkg = importPackage("RemoteCopyStick_DoSomething.zip");

        assertEquals(PackageType.CopyStick, pkg.type);
        assertEquals("DoSomething", pkg.version);
        assertEquals(1, pkg.size.files);
        assertTrue(pkg.slot == 0);
    }

    @Test
    public void importsIndis5MultimediaContent() throws Exception {
        Package pkg = importPackage("indis5-content.zip");

        assertEquals(PackageType.Indis5MultimediaContent, pkg.type);
        assertEquals("DNOW-1.0", pkg.version);
        assertEquals(5, pkg.size.files);
        assertEquals(202526, pkg.size.bytes);
        assertTrue(pkg.slot == 0);
    }

    @Test
    public void importsIndis3MultimediaContent() throws Exception {
        Package pkg = importPackage("Indis3-Content.zip");

        assertEquals(PackageType.Indis3MultimediaContent, pkg.type);
        assertEquals("1.0", pkg.version);
        assertEquals(5, pkg.size.files);
        assertEquals(38534, pkg.size.bytes);
        assertTrue(pkg.slot == 0);
    }

    @Test
    public void importsXccEnnoSeatReservation() throws Exception {
        Package pkg = importPackage("XccEnnoSitzplatz_UIC_1440120.zip");
        assertEquals(PackageType.XccEnnoSeatReservation, pkg.type);
        assertEquals("UIC_1440120", pkg.version);
        assertEquals(1, pkg.size.files);
        assertEquals(31933, pkg.size.bytes);
        assertTrue(pkg.slot == 0);
    }

    @Test
    public void importsOebbDigitalContentPackage() throws Exception {
        Package pkg = importPackage("OebbDigitalContent_1.0.zip");
        assertEquals(PackageType.OebbDigitalContent, pkg.type);
        assertEquals("28.04.2017 08:22:41", pkg.version);
        assertEquals(2, pkg.size.files);
        assertEquals(2700, pkg.size.bytes);
        assertTrue(pkg.slot == 0);
    }

    @Test
    public void importsPassengerTvContent() throws Exception {
        Package pkg = importPackage("Fahrgast-TV-Content_1.0.zip");
        assertEquals(PackageType.PassengerTvContent, pkg.type);
        assertEquals("1.0", pkg.version);
        assertEquals(4, pkg.size.files);
        assertEquals(74076, pkg.size.bytes);
        assertTrue(pkg.slot == 0);
    }

    @Test
    public void runsPreprocessingIfNeeded() throws Exception {
        String fileName = "Fahrgast-TV-Content_1.0.zip";
        PreprocessSetting setting = mock(PreprocessSetting.class);
        when(preprocessor.needPreprocessing(fileName)).thenReturn(setting);
        when(preprocessor.preprocess(eq(setting), eq(fileName), any(), any())).thenReturn(TestFiles.find(fileName));

        Package pkg = importPackage(fileName);

        verify(preprocessor).needPreprocessing(fileName);
        verify(preprocessor).preprocess(eq(setting), eq(fileName), any(), any());
    }

    @Test(expected = ImportPackage.WrongPackageType.class)
    public void exceptionWhenFileIsNotPackageOfSpecifiedType() throws Exception {
        String fileName = "Fahrgast-TV-Content_1.0.zip";
        PreprocessSetting setting = new PreprocessSetting(PackageType.DataSupply, null, null, null);
        when(preprocessor.needPreprocessing(fileName)).thenReturn(setting);
        when(preprocessor.preprocess(eq(setting), eq(fileName), any(), any())).thenReturn(TestFiles.find(fileName));

        Package pkg = importPackage(fileName);

        verify(preprocessor).preprocess(eq(setting), eq(fileName), any(), any());
    }

    @Test
    public void noPreprocessing_IfNotNeeded() throws Exception {
        String fileName = "Fahrgast-TV-Content_1.0.zip";
        when(preprocessor.needPreprocessing(fileName)).thenReturn(null);

        Package pkg = importPackage(fileName);

        verify(preprocessor, never()).preprocess(any(), any(), any());
    }

    @Test
    public void importsSbhOutageTicker() throws Exception {
        Package pkg = importPackage("ET474-Outage-Ticker_1.0.zip");

        assertEquals(PackageType.SbhOutageTicker, pkg.type);
        assertEquals("1.0", pkg.version);
        assertEquals(1, pkg.size.files);
        assertEquals(682, pkg.size.bytes);
        assertTrue(pkg.slot == 0);
    }

    private Package importPackage(String file) throws Exception {
        try (FileInputStream fis = new FileInputStream(TestFiles.find(file))) {
            return tested.importPackage(file, fis, IMPORT_SOURCE, null);
        }
    }

}
