package fleetmanagement.backend.repositories.memory;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import fleetmanagement.backend.diagnosis.*;
import fleetmanagement.backend.diagnosis.DiagnosisRepository;
import fleetmanagement.backend.repositories.exception.DiagnosisDuplicationException;

public class InMemoryDiagnosisRepository implements DiagnosisRepository {

	private final List<Diagnosis> diagnoses = new ArrayList<>();
	private DiagnosisHistoryRepository historyRepository;

	public InMemoryDiagnosisRepository(DiagnosisHistoryRepository historyRepository) {
		this.historyRepository = historyRepository;
	}

	@Override
	public List<Diagnosis> listAll() {
		return diagnoses;
	}

	@Override
	public Map<UUID, Diagnosis> mapAll() {
		Map<UUID, Diagnosis> map = new HashMap<>();
		for (Diagnosis diagnosis : diagnoses)
			map.put(diagnosis.getVehicleId(), diagnosis);
		return map;
	}

	@Override
	public List<StateEntry> getDiagnosedDeviceHistory(UUID vehicleId, String deviceId) {
		return historyRepository.getHistory(vehicleId, deviceId);
	}

	@Override
	public StateEntry getLatestDeviceHistoryRecord(UUID vehicleId, String deviceId) {
		return historyRepository.getLatestHistory(vehicleId, deviceId);
	}

	@Override
	public List<StateEntry> getDeviceHistoryRange(UUID vehicleId, String deviceId, ZonedDateTime start) {
		return historyRepository.getHistoryRange(vehicleId, deviceId, start, start);
	}

	@Override
	public void insertDeviceHistory(UUID vehicleId, String id, StateEntry state) {
		historyRepository.addHistory(vehicleId, id, state);
	}

	@Override
	public void insertDeviceHistory(UUID vehicleId, String id, List<StateEntry> states) {
		historyRepository.addHistory(vehicleId, id, states);
	}

	@Override
	public void deleteDeviceHistory(UUID vehicleId, String id) {
		historyRepository.delete(vehicleId, id);
	}

	@Override
	public void deleteDeviceHistory(UUID vehicleId, String id, StateEntry state) {
		historyRepository.delete(vehicleId, id, state);
	}

	@Override
	public void insert(Diagnosis diagnosis) {
		if (!existsInList(diagnosis))
			diagnoses.add(diagnosis);
		else 
			throw new DiagnosisDuplicationException(diagnosis.getVehicleId());
	}

	@Override
	public void delete(UUID vehicleId) {
		Diagnosis toRemove = tryFindByVehicleId(vehicleId);
		if (toRemove != null)
			diagnoses.remove(toRemove);
	}

	@Override
	public Diagnosis tryFindByVehicleId(UUID vehicleId) {
		return diagnoses.stream().filter(d -> d.getVehicleId().equals(vehicleId)).findFirst().orElse(null);
	}

	@Override
	public void update(UUID vehicleId, Consumer<Diagnosis> update) {
		Diagnosis diagnosis = tryFindByVehicleId(vehicleId);
		update.accept(diagnosis);
	}

	public void update(Diagnosis d) {
		delete(d.getVehicleId());
		insert(d);
	}
	
	private boolean existsInList(Diagnosis diagnosis) {
		return !diagnoses.stream().filter(d -> d.getVehicleId().equals(diagnosis.getVehicleId())).collect(Collectors.toList()).isEmpty();
	}
}
