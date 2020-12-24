package fleetmanagement.frontend.controllers;

import fleetmanagement.TempFileRule;
import fleetmanagement.config.CommandsGenerator;
import fleetmanagement.config.Licence;
import fleetmanagement.config.LicenceImpl;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.SocketException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LicenceControllerTest {

    LicenceController tested;
    Licence licence;
    @Rule
    public TempFileRule tempDir = new TempFileRule();


    @Before
    public void before() throws SocketException, UnknownHostException {
        TestScenario testScenario = new TestScenario();
        licence = new LicenceImpl(tempDir);
        tested = new LicenceController(new SessionStub(), licence, testScenario.vehicleRepository);
    }


    @Test
    public void doesNotCreateLicenceFile_IfInvalidCommandSended() {
        String invalidJsonCommand = "{'languages': ['fr' 'ru']}";
        String command = CommandsGenerator.encrypt(licence.getInstallationSeed(), invalidJsonCommand);
        tested.addon(command);

        assertFalse(tempDir.append("licence.txt").exists());
    }

    @Test
    public void createsLicenceFile_IfLicenceUpdated() {
        String invalidJsonCommand = "{'vehicles' : 20}";
        String command = CommandsGenerator.encrypt(licence.getInstallationSeed(), invalidJsonCommand);
        tested.addon(command);

        assertTrue(tempDir.append("licence.txt").exists());
    }


}