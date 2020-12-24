package fleetmanagement;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageSize;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.frontend.UserSession;
import org.mockito.Mockito;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.mockito.Mockito.when;

public final class TestObjectFactory {

    private TestObjectFactory() {
    }

    public static Package createPackage(PackageType type, String version) {
        return createPackage(type, version, null, null, null);
    }

    public static Package createPackage(PackageType type, String version, Integer slot, String startPeriod, String endPeriod) {
        Package pkg = new Package(UUID.randomUUID(), type, version, null,
                new PackageSize(2, 1024), slot, startPeriod, endPeriod);
        pkg.source = "Source: Unit test";
        return pkg;
    }

    public static Vehicle createVehicle(String name) {
        return new Vehicle(name, null, name, "1.2.34567.0", ZonedDateTime.now(), null, false, 1);
    }

    public static UserSession userSessionWithLocale() {
        UserSession session = Mockito.mock(UserSession.class);
        when(session.getLocale()).thenReturn(Locale.ENGLISH);
        return session;
    }

}
