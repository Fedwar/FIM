package fleetmanagement.backend.notifications;

import fleetmanagement.backend.notifications.settings.Type;
import org.junit.Test;

import static org.junit.Assert.*;

public class TypeTest {

    @Test
    public void getByResourceKey() {
        for (Type type : Type.values()) {
            Type typeByResourceKey = Type.getByResourceKey(type.getResourceKey());
            assertEquals(type, typeByResourceKey);
        }
    }
}