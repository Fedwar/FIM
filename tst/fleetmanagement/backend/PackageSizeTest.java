package fleetmanagement.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import fleetmanagement.backend.packages.PackageSize;


public class PackageSizeTest {
	private PackageSize tested;
	
	@Before
	public void setup() {
		tested = new PackageSize(1, 2);
	}
	
	@Test
	public void createsStringRepresentation() {
		assertFalse(tested.toString().isEmpty());
	}
	
	@Test
	public void implementsEquals() {
		PackageSize same = new PackageSize(1, 2);
		PackageSize different = new PackageSize(2, 4);
		assertEquals(same, tested);
		assertNotEquals(different, tested);
		assertTrue(same.hashCode() == tested.hashCode());
		assertFalse(different.hashCode() == tested.hashCode());
	}
}
