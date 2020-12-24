package fleetmanagement.usecases;

import fleetmanagement.TestUtils;
import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.tasks.TaskStatus.ClientStage;
import fleetmanagement.backend.tasks.TaskStatus.ServerStatus;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.PackageDetails;
import fleetmanagement.frontend.model.VehicleGroupMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static fleetmanagement.TestObjectFactory.createPackage;
import static fleetmanagement.TestObjectFactory.createVehicle;
import static fleetmanagement.TestObjectFactory.userSessionWithLocale;
import static fleetmanagement.TestUtils.simulateInstallation;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ListPackageDetailsTest {

	@Mock
	private VehicleRepository vehicleRepository;
	@Mock
	private TaskRepository taskRepository;
	@Mock
	private GroupRepository groupRepository;
	@Mock
	private PackageRepository packageRepository;

	private Package pkg = createPackage(PackageType.DataSupply, "1.0", 1, "08.09.2013 00:00:00", "01.12.2013 23:59:59");
	private Vehicle vehicle = createVehicle("Vehicle 1");
	private Group group1 = new Group("group1","group1",true);
	private Group group2 = new Group("group2","group2",true);


	private UserSession request = userSessionWithLocale();

	private ListPackageDetails listPackageDetails;

	@Before
	public void setup() {
		when(vehicleRepository.listAll()).thenReturn(Collections.singletonList(vehicle));
		when(groupRepository.listAll()).thenReturn(Arrays.asList(group1, group2));
		when(groupRepository.mapAll()).thenReturn(TestUtils.toGroupMap(group1, group2));

		listPackageDetails = new ListPackageDetails(vehicleRepository, taskRepository, groupRepository, packageRepository);
	}

	@Test
	public void fillsPackageProperties() {
		when(packageRepository.isGroupContainsPackageDuplicate(pkg, group1)).thenReturn(true);
		pkg.groupId = group1.id;
		when(groupRepository.tryFindById(group1.id)).thenReturn(group1);

		PackageDetails details = listPackageDetails.listPackageDetails(pkg, request);

		assertEquals(pkg.version, details.version);
		assertEquals(pkg.slot.toString(), details.slot);
		assertEquals("Data Supply 1.0 (Slot 1)", details.name);
		assertEquals("Data Supply", details.type);
		assertEquals("1 KB, 2 files", details.size);
		assertEquals(pkg.id.toString(), details.key);
		assertEquals(pkg.startOfPeriod, details.startOfPeriod);
		assertEquals(pkg.endOfPeriod, details.endOfPeriod);

		assertEquals(group1.id.toString(), details.groupId);
		assertEquals(group1.name, details.groupName);

		assertEquals(1, details.groupsForAssigning.size());
		assertEquals(group2.name, details.groupsForAssigning.get(group2.id.toString()));
		assertEquals(1, details.groupsForRemovingPackagesFromThem.size());
		assertEquals(group1.name, details.groupsForRemovingPackagesFromThem.get(group1.id.toString()));

		assertTrue(details.vehicleMapForInstallation.getVehiclesByGroup().isEmpty());

		assertThat(details.downloadAvailable, is(false));
	}

	@Test
	public void archiveDownloadAvailable() {
		pkg.archive = new File("f");

		PackageDetails details = listPackageDetails.listPackageDetails(pkg, request);

		assertThat(details.downloadAvailable, is(true));
	}

	@Test
	public void vehicleMapForInstallation_notEmptyForGroup() {
		pkg.groupId = group1.id;
		when(groupRepository.tryFindById(group1.id)).thenReturn(group1);
		vehicle.setGroupId(group1.id.toString());
		when(vehicleRepository.listByGroup(group1.id.toString())).thenReturn(Collections.singletonList(vehicle));

		PackageDetails details = listPackageDetails.listPackageDetails(pkg, request);

		assertThat(details.installedCount, is(0));
		assertThat(details.vehicleCount, is(1));

		Map<String, List<VehicleGroupMap.VehicleDTO>> vehiclesByGroup = details.vehicleMapForInstallation.getVehiclesByGroup();
		assertThat(vehiclesByGroup.size(), is(1));
		assertThat(vehiclesByGroup.get(group1.name), notNullValue());
		assertThat(vehiclesByGroup.get(group1.name).size(), is(1));
		assertThat(vehiclesByGroup.get(group1.name).get(0).id, is(vehicle.id.toString()));
	}

	@Test
	public void handlesNoInstallationIsRunning() {
		PackageDetails details = listPackageDetails.listPackageDetails(pkg, request);

		assertThat(details.installationInProgress, is(false));
		assertThat(details.installationProgressPercent, is(0));
		assertThat(details.installationStartedAt, nullValue());
		assertThat(details.installationEstimatedCompletion, is("unknown"));
		assertThat(details.installedCount, is(0));
		assertThat(details.vehicleCount, is(0));

		Map<String, List<VehicleGroupMap.VehicleDTO>> vehiclesByGroup = details.vehicleMapForInstallation.getVehiclesByGroup();
		assertThat(vehiclesByGroup.size(), is(1));
		assertThat(vehiclesByGroup.get(""), notNullValue());
		assertThat(vehiclesByGroup.get("").size(), is(1));
		assertThat(vehiclesByGroup.get("").get(0).id, is(vehicle.id.toString()));
	}
	
	@Test
	public void handlesInstallationIsRunning() {
		simulateInstallation(pkg, vehicle, 85, taskRepository);

		PackageDetails details = listPackageDetails.listPackageDetails(pkg, request);

		assertThat(details.installationInProgress, is(true));
		assertThat(details.installationProgressPercent, is(85));
		assertThat(details.installationStartedAt, notNullValue());
		assertThat(details.installationEstimatedCompletion, notNullValue());
		assertThat(details.installedCount, is(0));
		assertThat(details.vehicleCount, is(1));

		Map<String, List<VehicleGroupMap.VehicleDTO>> vehiclesByGroup = details.vehicleMapForInstallation.getVehiclesByGroup();
		assertTrue(vehiclesByGroup.isEmpty());
	}
	
	@Test
	public void handlesInstallationIsFinished() {
		vehicle.versions.setDataSupplyVersion(pkg.slot, pkg.version);
		
		PackageDetails details = listPackageDetails.listPackageDetails(pkg, request);

		assertThat(details.installationInProgress, is(false));
		assertThat(details.installedCount, is(1));
		assertThat(details.vehicleCount, is(1));
		assertThat(details.installedVehicles.size(), is(1));
		assertThat(details.installedVehicles.get(0).key, is(vehicle.id.toString()));
	}
}
