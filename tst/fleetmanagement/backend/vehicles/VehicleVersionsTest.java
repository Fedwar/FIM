package fleetmanagement.backend.vehicles;

import static org.junit.Assert.*;

import org.junit.*;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.vehicles.VehicleVersions.Versioned;


public class VehicleVersionsTest {
	private VehicleVersions tested;
	
	@Before
	public void setup() {
		tested = new VehicleVersions();
	}
	
	@Test
	public void setsDataSupplyVersionsPerSlot() {
		tested.setDataSupplyVersion(1, "1.0");
		tested.setDataSupplyVersion(2, "1.1");
		
		assertEquals("1.0", tested.getDataSupplyVersion(1));
		assertEquals("1.1", tested.getDataSupplyVersion(2));
	}
	
	@Test
	public void overwritesDataSupplyVersionForSlot() {
		tested.setDataSupplyVersion(1, "1.0");
		tested.setDataSupplyVersion(1, "1.1");
		
		assertEquals("1.1", tested.getDataSupplyVersion(1));
	}
	
	@Test
	public void storesVersions() {
		Versioned versioned = new Versioned(PackageType.DataSupply, 1, "1.0");
		tested.add(versioned);
		
		assertEquals("1.0", tested.get(PackageType.DataSupply, 1).version);
	}
	
	@Test
	public void storesPackageVersions() {
		tested.set(PackageType.OebbDigitalContent, "1.0");
		
		assertEquals("1.0", tested.get(PackageType.OebbDigitalContent).version);
	}
}
