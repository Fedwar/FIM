package fleetmanagement.config;

import fleetmanagement.TempFileRule;
import fleetmanagement.backend.packages.PackageType;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Period;
import java.time.ZonedDateTime;

import static org.junit.Assert.*;

public class LicenceImplTest {

    LicenceImpl licence;
    @Rule
    public TempFileRule tempDir = new TempFileRule();

    @Before
    public void before() throws SocketException, UnknownHostException {
        licence = new LicenceImpl(tempDir);
    }

    @Test
    public void whenLicenceInfoIsNull_ThenLicenceExpired() throws Exception {
        FieldUtils.writeField(licence, "licenceInfo", null, true);

        assertTrue(licence.isExpired());
    }

    @Test
    public void whenExpireDateIsNull_ThenEndlessLicence() throws Exception {
        LicenceInfo licenceInfo = new LicenceInfo(null, 0, false, false, null, false, false, false, false, false, false, false, null, false);
        FieldUtils.writeField(licence, "licenceInfo", licenceInfo, true);

        assertFalse(licence.isExpired());
    }

    @Test
    public void isExpired() throws Exception {
        ZonedDateTime expireDate = ZonedDateTime.now().plus(Period.ofDays(1));
        LicenceInfo licenceInfo = new LicenceInfo(expireDate, 0, false, false, null, false, false, false, false, false, false, false, null, false);
        FieldUtils.writeField(licence, "licenceInfo", licenceInfo, true);

        assertFalse(licence.isExpired());

        expireDate = ZonedDateTime.now().minus(Period.ofDays(1));
        licenceInfo = new LicenceInfo(expireDate, 0, false, false, null, false, false, false, false, false, false,false, null, false);
        FieldUtils.writeField(licence, "licenceInfo", licenceInfo, true);

        assertTrue(licence.isExpired());
    }

    @Test
    public void whenLicenceInfoIsNull_ThenVehicleCountZero() throws Exception {
        FieldUtils.writeField(licence, "licenceInfo", null, true);

        assertEquals(0, licence.getMaximumVehicleCount());
    }

    @Test
    public void whenLicenceInfoIsNull_ThenNoAddons() throws Exception {
        FieldUtils.writeField(licence, "licenceInfo", null, true);

        assertFalse(licence.isVehicleGeoAvailable());
        assertFalse(licence.isMapAvailable());
        assertFalse(licence.isDiagnosisInfoAvailable());
        assertFalse(licence.isPackageTypeAvailable(PackageType.DataSupply));
    }

    @Test
    public void getMaximumVehicleCount() throws Exception {
        int vehicleCount = 10;
        LicenceInfo licenceInfo = new LicenceInfo(ZonedDateTime.now().plus(Period.ofYears(1)), vehicleCount, false, false, null, false, false, false, false, false, false, false, null, false);
        FieldUtils.writeField(licence, "licenceInfo", licenceInfo, true);

        assertEquals(10, licence.getMaximumVehicleCount());
    }

    @Test
    public void doesNotAcceptInvalidCommand() {
        String invalidJsonCommand = "{'languages': ['fr' 'ru']}";
        String command = CommandsGenerator.encrypt(licence.getInstallationSeed(), invalidJsonCommand);

        assertFalse(licence.update(command));
    }




}