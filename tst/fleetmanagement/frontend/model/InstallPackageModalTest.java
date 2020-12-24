package fleetmanagement.frontend.model;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.*;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.frontend.model.InstallPackageModal.*;
import fleetmanagement.test.*;


public class InstallPackageModalTest {
	
	private InstallPackageModal tested;
	private TestScenarioPrefilled scenario;
	private SessionStub request;
	
	@Before
	public void setUp() {
		request = new SessionStub();
		scenario = new TestScenarioPrefilled();
	}

	private void createTested() {
		tested = new InstallPackageModal(scenario.vehicle1, scenario.packageRepository.listAll(), scenario.taskRepository, request);
	}
	
	@Test
	public void recognizesEmptyPackageList() {
		tested = new InstallPackageModal(scenario.vehicle1, Collections.emptyList(), scenario.taskRepository, request);
		
		assertFalse(tested.isAtLeastOnePackageInstallable);
		assertEquals(0, tested.packageTypes.size());
	}
	
	@Test
	public void listsPackagesByPackageType() {
		scenario.addPackage(PackageType.CopyStick, "1.2");
		
		createTested();
		
		assertTrue(tested.isAtLeastOnePackageInstallable);
		
		InstallablePackageType dataSupplyPackages = tested.packageTypes.get(0);
		assertEquals("Data Supply", dataSupplyPackages.name);
		assertEquals(3, dataSupplyPackages.installablePackages.size());
		
		InstallablePackageType copyStickPackages = tested.packageTypes.get(1);
		assertEquals("Remote CopyStick", copyStickPackages.name);
		assertEquals(3, copyStickPackages.installablePackages.size());
	}

	@Test
	public void fillsPackageDetails() {
		createTested();
		
		assertTrue(tested.isAtLeastOnePackageInstallable);
		
		InstallablePackage dataSupplyPackage = tested.packageTypes.get(0).installablePackages.get(0);
		assertTrue(dataSupplyPackage.isInstallable);
		assertEquals("Data Supply 1.0 (Slot 1)", dataSupplyPackage.name);
		assertEquals(scenario.package1.id.toString(), dataSupplyPackage.packageId);
		assertEquals("Not installed", dataSupplyPackage.status);
	}
	
	@Test
	public void recognizesInstallationStatus() {
		scenario.vehicle1.versions.setDataSupplyVersion(1, "1.0");
		Package package3 = scenario.addPackage(PackageType.DataSupply, "1.2", 1, "08.09.2013 00:00:00", "01.12.2013 23:59:59");
		scenario.addTask(scenario.vehicle1, package3);
		createTested();
		
		InstallablePackageType dataSupplyPackages = tested.packageTypes.get(0);
		assertEquals("Installed", dataSupplyPackages.installablePackages.get(0).status);
		assertTrue(dataSupplyPackages.installablePackages.get(0).isInstallable);
		assertEquals("Not installed", dataSupplyPackages.installablePackages.get(1).status);
		assertTrue(dataSupplyPackages.installablePackages.get(1).isInstallable);
		assertEquals("Installation running (0%)", dataSupplyPackages.installablePackages.get(3).status);
		assertFalse(dataSupplyPackages.installablePackages.get(3).isInstallable);
	}

}
