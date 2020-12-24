package fleetmanagement.backend.vehiclecommunication;

import fleetmanagement.backend.vehiclecommunication.upload.exceptions.UploadFileNotLicenced;

import java.util.UUID;

public interface FileUploadListener {
	boolean canHandleUploadedFile(String filename);
	void onFileUploaded(UUID vehicleId, String filename, byte[] data) throws UploadFileNotLicenced;
}