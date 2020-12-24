package fleetmanagement.backend.repositories.disk;

import fleetmanagement.TempFileRule;
import fleetmanagement.backend.installations.PackageInstallation;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.vehicles.Vehicle;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static fleetmanagement.TestObjectFactory.createPackage;
import static fleetmanagement.TestObjectFactory.createVehicle;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PackageInstallationSqlRepositoryTest {

    private PackageInstallationSqlRepository instance;

    private PackageInstallation packageInstallation;

    @Rule
    public TempFileRule repositoryDir = new TempFileRule();


    @Before
    public void setUp() throws Exception {
        instance = new PackageInstallationSqlRepository(repositoryDir);

        Package p1 = createPackage(PackageType.DataSupply, "v1");
        Vehicle v1 = createVehicle("vehicle1");
        Task t1 = new Task(p1, v1, null);
        packageInstallation = new PackageInstallation(UUID.randomUUID(), Collections.singletonList(t1));
        packageInstallation.setStartDatetime(new Date(1570000660000L));
        packageInstallation.setEndDatetime(new Date(1570000770000L));
    }

    @Test
    public void writeAndRead() {
        instance.insert(packageInstallation);

        PackageInstallation loaded = instance.tryFindById(packageInstallation.id());

        assertThat(loaded, not(sameInstance(packageInstallation)));
        assertThat(loaded.id(), is(packageInstallation.id()));
        assertThat(loaded.getConflictingTasks().isEmpty(), is(true));
        assertThat(loaded.getStartDatetime().getTime(), is(packageInstallation.getStartDatetime().getTime()));
        assertThat(loaded.getEndDatetime().getTime(), is(packageInstallation.getEndDatetime().getTime()));
        assertThat(loaded.getTasks().size(), is(1));
        assertThat(loaded.getTasks().get(0), is(packageInstallation.getTasks().get(0)));

        List<PackageInstallation> loadedList = instance.listAll();
        assertThat(loadedList.size(), is(1));
        assertThat(loadedList.get(0).id(), is(packageInstallation.id()));
    }

    @Test
    public void update() throws ParseException {
        instance.insert(packageInstallation);

        Date newStartDate = new Date(1570000880000L);
        Date newEndDate = new Date(1570000990000L);

        instance.update(packageInstallation.id(), i -> {
            i.setStartDatetime(newStartDate);
            i.setEndDatetime(newEndDate);
        });

        PackageInstallation loaded = instance.tryFindById(packageInstallation.id());

        assertThat(loaded.getStartDatetime().getTime(), is(newStartDate.getTime()));
        assertThat(loaded.getEndDatetime().getTime(), is(newEndDate.getTime()));
    }

    @Test
    public void delete() {
        instance.insert(packageInstallation);

        instance.delete(packageInstallation.id());

        assertThat(instance.tryFindById(packageInstallation.id()), nullValue());

        assertTrue(instance.listAll().isEmpty());
    }
}