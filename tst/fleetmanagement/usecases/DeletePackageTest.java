package fleetmanagement.usecases;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.UUID;

import org.junit.*;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.test.TestScenarioPrefilled;


public class DeletePackageTest {
	
	private DeletePackage tested;
	private TestScenarioPrefilled scenario;
	
	@Before
	public void setup() {
		scenario = new TestScenarioPrefilled();
		tested = new DeletePackage(scenario.packageRepository, scenario.taskRepository, scenario.vehicleRepository
				, scenario.groupRepository);
	}
	
	@Test
	public void ignoresNonExistantPackages() throws IOException {
		tested.deleteById(UUID.randomUUID(), null);
	}
	
	@Test
	public void deletesPackageInRepository() throws Exception {
		Package toDelete = scenario.package1;
		tested.deleteById(toDelete.id, null);
		assertFalse(scenario.packageRepository.listAll().contains(toDelete));
	}
	
	@Test
	public void deletesAssociatedTasksWhenDeletingPacking() throws Exception {
		Package toDelete = scenario.package1;
		Task toCancel = scenario.addTask(scenario.vehicle1, toDelete);
		tested.deleteById(toDelete.id, null);
		assertTrue(scenario.vehicle1.getTaskIds().isEmpty());
		assertNull(scenario.taskRepository.tryFindById(toCancel.getId()));
	}
}
