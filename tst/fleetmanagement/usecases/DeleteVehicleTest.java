package fleetmanagement.usecases;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.UUID;

import org.junit.*;

import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenarioPrefilled;


public class DeleteVehicleTest {
	
	private DeleteVehicle tested;
	private TestScenarioPrefilled scenario;
	
	@Before
	public void setup() {
		scenario = new TestScenarioPrefilled();
		tested = new DeleteVehicle(scenario.vehicleRepository, scenario.taskRepository, scenario.diagnosisRepository);
	}
	
	@Test
	public void deletesVehicleInRepository() throws Exception {
		Vehicle toDelete = scenario.vehicle1;
		tested.deleteById(toDelete.id);
		assertFalse(scenario.vehicleRepository.listAll().contains(toDelete));
	}
	
	@Test
	public void deletesAssociatedDiagnosis() throws Exception {
		UUID vehicleId = scenario.vehicle1.id;
		assertNotNull(scenario.diagnosisRepository.tryFindByVehicleId(vehicleId));
		
		tested.deleteById(vehicleId);
		
		assertNull(scenario.diagnosisRepository.tryFindByVehicleId(vehicleId));
	}
	
	@Test
	public void ignoresNonExistantVehicles() throws IOException {
		tested.deleteById(UUID.randomUUID());
	}

	@Test
	public void deletesAssociatedTasksWhenDeletingPacking() throws Exception {
		Vehicle toDelete = scenario.vehicle1;
		Task toCancel = scenario.addTask(toDelete, scenario.package1);
		tested.deleteById(toDelete.id);
		assertNull(scenario.taskRepository.tryFindById(toCancel.getId()));
	}
}
