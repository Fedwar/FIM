package fleetmanagement.backend.vehiclecommunication.upload;

import java.nio.charset.Charset;
import java.time.ZonedDateTime;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.backend.packages.sync.PackageSyncService;
import fleetmanagement.backend.repositories.migration.DistinguishBetweenIndis3AndIndis5MultimediaContent;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.*;
import fleetmanagement.config.Licence;
import gsp.util.DoNotObfuscate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MultimediaContentVersionFileUploadListener extends TypicalVehicleUploadListener {

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private final Gson gson = new Gson();
	private final PackageSyncService packageSyncService;

	public MultimediaContentVersionFileUploadListener(@Autowired VehicleRepository vehicles, @Autowired PackageSyncService packageSyncService) {
		super(vehicles);
		this.packageSyncService = packageSyncService;
	}
	
	@Override
	public boolean canHandleUploadedFile(String filename) {
		return filename.equals("multimedia-content-version.json");
	}
	
	@Override
	public void onFileUploaded(Vehicle sender, String filename, byte[] data) {		
		MultimediaContentVersion version = gson.fromJson(new String(data, UTF8), MultimediaContentVersion.class);
		if (version.version != null && version.version.isEmpty())
			version.version = null;
		
		PackageType multimediaPackageType = DistinguishBetweenIndis3AndIndis5MultimediaContent.guessTypeFromVehicleName(sender.getName());
		sender.versions.set(multimediaPackageType, version.version);
		sender.lastSeen = ZonedDateTime.now();

		packageSyncService.syncPackages(sender, multimediaPackageType);
	}
	
	@DoNotObfuscate
	public static class MultimediaContentVersion {
		@SerializedName("multimedia-content-version") String version; 
	}


}
