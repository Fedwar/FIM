package fleetmanagement.backend.vehiclecommunication.upload.filter;

import fleetmanagement.backend.repositories.Repository;

import java.util.UUID;
import java.util.function.Consumer;

public interface FilterSequenceRepository extends Repository<UploadFilterSequence, UUID> {

    UploadFilterSequence findByType(FilterType type);

    void createFilterDirectories();

    void updateOrInsert(Consumer<UploadFilterSequence> consumer);
}