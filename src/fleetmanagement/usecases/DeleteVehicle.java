package fleetmanagement.usecases;

import java.util.UUID;

import fleetmanagement.backend.diagnosis.DiagnosisRepository;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeleteVehicle {

	@Autowired
	private VehicleRepository vehicleRepository;
	@Autowired
	private TaskRepository taskRepository;
	@Autowired
	private DiagnosisRepository diagnosisRepository;

	public DeleteVehicle() {
	}

	public DeleteVehicle(VehicleRepository vehicleRepository, TaskRepository taskRepository, DiagnosisRepository diagnosisRepository) {
		this.vehicleRepository = vehicleRepository;
		this.taskRepository = taskRepository;
		this.diagnosisRepository = diagnosisRepository;
	}

	public void deleteById(UUID id) {
		Vehicle vehicle = vehicleRepository.tryFindById(id);
		
		if (vehicle == null)
			return;

		removeVehicleItself(vehicle);
		removeAssociatedTasks(vehicle);
		removeAssociatedDiagnosis(vehicle);
	}

	private void removeAssociatedTasks(Vehicle vehicle) {
		vehicle.getTaskIds().forEach(this::removeTask);
	}

	private void removeTask(UUID taskId) {
		taskRepository.delete(taskId);
	}

	private void removeAssociatedDiagnosis(Vehicle vehicle) {
		diagnosisRepository.delete(vehicle.id);
	}

	private void removeVehicleItself(Vehicle vehicle) {
		vehicleRepository.delete(vehicle.id);
	}
}
