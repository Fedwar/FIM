package fleetmanagement.usecases;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import fleetmanagement.TestObjectFactory;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.frontend.UserSession;
import org.hamcrest.Matchers;
import org.junit.*;

import fleetmanagement.frontend.model.PackageList;
import fleetmanagement.frontend.model.PackageList.Category;
import fleetmanagement.test.*;

import java.util.List;

//todo redo
public class ListPackagesTest {
	private TestScenarioPrefilled scenario;
	private UserSession request = TestObjectFactory.userSessionWithLocale();

	@Before
	public void setup() throws Exception {
		scenario = new TestScenarioPrefilled();
	}

	@Test
	public void listsPackagesByCategory() throws Exception {
		ListPackages tested = new ListPackages(scenario.packageRepository.listAll(), scenario.vehicleRepository, scenario.taskRepository);
		PackageList vm = tested.listPackages(request, scenario.groupRepository);
		assertEquals(2, vm.categories.size());

		Category c = vm.categories.get(0);
		assertEquals("Data Supply", c.name);
		assertEquals(3, c.packages.size());
	}

	@Test
	public void validityPeriodAvailableForDataSupplyWithSlotGreaterThan0() throws Exception {
		scenario.addPackage(PackageType.DataSupply, "1.0", 0, "08.09.2013 00:00:00", "01.12.2013 23:59:59");

		ListPackages tested = new ListPackages(scenario.packageRepository.listAll(), scenario.vehicleRepository, scenario.taskRepository);
		PackageList vm = tested.listPackages(request, scenario.groupRepository);

		List<PackageList.Entry> dataSupplyEntries = vm.categories.get(0).packages;
		List<PackageList.Entry> otherTypeEntries = vm.categories.get(1).packages;
		PackageList.Entry dataSupplyWithSlot0 = dataSupplyEntries.stream()
				.filter(entry -> entry.slot == 0)
				.findFirst().orElse(null);
		PackageList.Entry dataSupplyWithSlot1 = dataSupplyEntries.stream()
				.filter(entry -> entry.slot == 1)
				.findFirst().orElse(null);

		assertNotNull( dataSupplyWithSlot1.validityStatus);
		assertNull(dataSupplyWithSlot0.validityStatus);
		assertTrue(otherTypeEntries.stream().allMatch(entry -> entry.validityStatus == null));
	}



	@Test
	public void listsPackageDetails() throws Exception {
		scenario.addTask(scenario.vehicle1, scenario.package1);

		ListPackages tested = new ListPackages(scenario.packageRepository.listAll(), scenario.vehicleRepository, scenario.taskRepository);
		PackageList vm = tested.listPackages(request, scenario.groupRepository);
		
		fleetmanagement.frontend.model.PackageList.Entry p = vm.categories.get(0).packages.get(0);
		assertEquals(scenario.package1.id.toString(), p.key);
		assertEquals("Data Supply 1.0 (Slot 1)", p.name);
		assertEquals(scenario.package1.version, p.version);
		assertThat(p.installedCount, is(0));
		assertThat(p.vehicleCount, is(1));
		assertEquals("", p.validity);
	}
}

