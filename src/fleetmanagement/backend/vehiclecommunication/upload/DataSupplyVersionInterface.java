package fleetmanagement.backend.vehiclecommunication.upload;

import java.util.List;

public interface DataSupplyVersionInterface {
    List<VersionFileUploadListener.Slot> getSlots();
}
