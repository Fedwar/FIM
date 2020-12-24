package fleetmanagement.backend.vehiclecommunication.upload.filter;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.vehicles.Vehicle;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class UploadFilterTest {

    static Group group;
    static Vehicle vehicle;
    static String fileName;
    static UploadFilterCondition trueCondition;
    static UploadFilterCondition falseCondition;

    UploadFilter tested;

    @BeforeClass
    public static void init() {
        vehicle = new Vehicle("test", "123", "test", "",
                ZonedDateTime.now(), null, false, 1);
        group = new Group("test", null, false);
        fileName = "test";

        trueCondition = spy(new UploadFilterCondition(ConditionType.VEHICLE_NAME, "*"));
        when(trueCondition.matches(anyString())).thenReturn(true);

        falseCondition = spy(new UploadFilterCondition(ConditionType.GROUP_NAME, "*"));
        when(falseCondition.matches(anyString())).thenReturn(false);
    }

    @Before
    public void setup() {
        tested = new UploadFilter();
    }

    @Test
    public void matchesSingleTrueConditions() {
        tested.addCondition(trueCondition);
        assertTrue(tested.matches(vehicle, group, fileName));
    }

    @Test
    public void notMatchesSingleFalseCondition() {
        tested.addCondition(falseCondition);
        assertFalse(tested.matches(vehicle, group, fileName));
    }

    @Test
    public void matchesMultipleTrueConditions() {
        tested.addCondition(trueCondition);
        tested.addCondition(trueCondition);
        tested.addCondition(trueCondition);
        assertTrue(tested.matches(vehicle, group, fileName));
    }

    @Test
    public void notMatchesMultipleConditionsWithFalseCondition() {
        tested.addCondition(trueCondition);
        tested.addCondition(trueCondition);
        tested.addCondition(falseCondition);
        assertFalse(tested.matches(vehicle, group, fileName));
    }

    @Test
    public void getCleanPath() {
        tested = new UploadFilter(null, "<vehicle>/folder/<data>", "", "Disabled", "30");
        assertEquals("", PathComposer.getCleanPath(tested));
    }
    @Test
    public void getCleanPath_doesNotCleanRelativeness() {
        tested = new UploadFilter(null, "./<vehicle>/folder/<data>", "", "Disabled", "30");
        assertEquals("./", PathComposer.getCleanPath(tested));
    }
    @Test
    public void getCleanPath_doesNotCleanAbsoluteness() {
        tested = new UploadFilter(null, "/<vehicle>/folder/<data>", "", "Disabled", "30");
        assertEquals("/", PathComposer.getCleanPath(tested));
    }
    @Test
    public void getCleanPath_CleansToFirstTag() {
        tested = new UploadFilter(null, "folder/name/<date>/<vehicle>", "", "Disabled", "30");
        assertEquals("folder/name/", PathComposer.getCleanPath(tested));
    }




}