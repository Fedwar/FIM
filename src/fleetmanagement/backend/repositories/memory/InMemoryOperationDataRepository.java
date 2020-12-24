package fleetmanagement.backend.repositories.memory;

import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.operationData.OperationDataHistoryRepository;
import fleetmanagement.backend.repositories.disk.OnDiskOperationDataRepository;

import java.io.File;

public class InMemoryOperationDataRepository extends OnDiskOperationDataRepository {

    public InMemoryOperationDataRepository(OperationDataHistoryRepository historyRepository) {
        super(null, historyRepository);
    }

    @Override
    public void loadFromDisk() {}

    @Override
    protected File getDirectory(OperationData persistable) {
        return null;
    }

    @Override
    protected void persist(OperationData operationData) {}

}
