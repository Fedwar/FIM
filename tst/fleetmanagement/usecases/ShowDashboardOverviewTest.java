package fleetmanagement.usecases;

import fleetmanagement.TestUtils;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehiclecommunication.upload.filter.FilterSequenceRepository;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilterSequence;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.Dashboard;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static fleetmanagement.TestObjectFactory.createPackage;
import static fleetmanagement.TestObjectFactory.createVehicle;
import static fleetmanagement.TestObjectFactory.userSessionWithLocale;
import static fleetmanagement.TestUtils.simulateInstallation;
import static fleetmanagement.backend.vehiclecommunication.upload.filter.FilterType.AD_FILTER_TYPE;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShowDashboardOverviewTest {
	
	@Mock
	private VehicleRepository vehicleRepository;
	@Mock
	private PackageRepository packageRepository;
	@Mock
	private TaskRepository taskRepository;
	@Mock
	private FilterSequenceRepository filterSequenceRepository;
	@Mock
	private Licence licence;
	
	private UserSession request = userSessionWithLocale();
	private Package pkg = createPackage(PackageType.DataSupply, "1.0", 1, "08.09.2013 00:00:00", "01.12.2013 23:59:59");
	private Package pkg2 = createPackage(PackageType.DataSupply, "1.0", 2, "08.09.2013 00:00:00", "01.12.2013 23:59:59");
	private Vehicle vehicle1 = createVehicle("vehicle1");
	private Vehicle vehicle2 = createVehicle("vehicle2");

	private ShowDashboardOverview tested;

	@Before
	public void setup() {
		when(filterSequenceRepository.findByType(AD_FILTER_TYPE)).thenReturn(new UploadFilterSequence(AD_FILTER_TYPE));
		when(packageRepository.listAll()).thenReturn(asList(pkg, pkg2));
		when(vehicleRepository.listAll()).thenReturn(asList(vehicle1, vehicle2));

		tested = new ShowDashboardOverview(
				vehicleRepository,
				packageRepository,
				taskRepository,
				filterSequenceRepository,
				licence
		);
	}	
	
	@Test
	public void gathersDashboardStatistics() {
		Dashboard d = createDashboard();
		
		assertEquals(2, d.statistics.packages);
		assertEquals(2, d.statistics.vehicles);
		assertEquals(4, d.statistics.totalPackageFiles);
		assertEquals("2 KB", d.statistics.totalPackageSize);
	}

	@Test
	public void summarizesRunningInstallations() {
		Package installed = pkg;
		simulateInstallation(pkg, vehicle1, 50, taskRepository);
		simulateInstallation(pkg, vehicle2, 100, taskRepository);
		vehicle2.versions.setDataSupplyVersion(installed.slot, installed.version);
		
		Dashboard d = createDashboard();
		
		assertEquals(1, d.runningInstallations.size());
		assertEquals("Data Supply 1.0 (Slot 1)", d.runningInstallations.get(0).name);
		assertEquals(pkg.id.toString(), d.runningInstallations.get(0).packageId);
		
		assertEquals(2, d.runningInstallations.get(0).totalInstallations);
		assertEquals(1, d.runningInstallations.get(0).finishedInstallations);
		assertEquals(75, d.runningInstallations.get(0).progress);
	}
	
	@Test
	public void summarizesDiagnosticErrors() {
		vehicle1.lastSeen = ZonedDateTime.now().minus(3, ChronoUnit.DAYS);
		
		Dashboard d = createDashboard();
		
		assertEquals(1, d.diagnosticErrors.size());
		assertEquals(vehicle1.getName(), d.diagnosticErrors.get(0).vehicleName);
		assertEquals(vehicle1.id.toString(), d.diagnosticErrors.get(0).vehicleId);
		assertEquals("No online connection for over two days", d.diagnosticErrors.get(0).description);
	}
	
	@Test
	public void showsSoftwareVersion() {
		Dashboard d = createDashboard();
		assertTrue(d.softwareVersion.length() > 0);
	}

	private Dashboard createDashboard() {
		return tested.createDashboard(request);
	}
	
}
