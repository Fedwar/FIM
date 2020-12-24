package fleetmanagement.backend.repositories.memory;

import fleetmanagement.backend.repositories.disk.OnDiskUploadFilterSequenceRepository;
import fleetmanagement.backend.settings.SettingsRepository;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilterSequence;
import fleetmanagement.config.Settings;

import java.io.File;

public class InMemoryUploadFilterSequenceRepository extends OnDiskUploadFilterSequenceRepository {

	public InMemoryUploadFilterSequenceRepository(Settings settings) {
		super(null, settings);
	}

	@Override
	public void loadFromDisk() {}

	@Override
	protected File getDirectory(UploadFilterSequence persistable) {
		return null;
	}

	@Override
	protected void persist(UploadFilterSequence persistable) {}


}
