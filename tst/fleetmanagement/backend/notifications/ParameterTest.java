package fleetmanagement.backend.notifications;

import fleetmanagement.backend.notifications.settings.Parameter;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParameterTest {

    @Test
    public void getByResourceKey() {
        for (Parameter parameter : Parameter.values()) {
            Parameter parameterByResourceKey = Parameter.getByResourceKey(parameter.getResourceKey());
            assertEquals(parameter, parameterByResourceKey);

        }
    }
}