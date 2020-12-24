package fleetmanagement.frontend.model;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.test.TestScenario;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Test;

import static org.junit.Assert.*;

public class AdminAutoSyncTest {

    @Test
    public void doesNotContainsTypesThatCantBeSynced() {
        TestScenario scenario = new TestScenario();
        AdminAutoSync tested = new AdminAutoSync(scenario.packageTypeRepository, scenario.licence);

        assertFalse(tested.packageTypes.keySet().contains(PackageType.CopyStick));
        assertFalse(tested.packageTypes.keySet().contains(PackageType.ClientConfig));
    }

}