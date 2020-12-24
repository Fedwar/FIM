package fleetmanagement.frontend.model;

import java.io.*;
import java.util.zip.*;

import org.apache.commons.io.IOUtils;

import fleetmanagement.backend.tasks.*;
import fleetmanagement.backend.vehicles.*;
import gsp.logging.*;

public class Logs {

	private final VehicleRepository vehicleRepository;
	private final TaskRepository tasks;

	public Logs(VehicleRepository vehicleRepository, TaskRepository tasks) {
		this.vehicleRepository = vehicleRepository;
		this.tasks = tasks;
	}

	public InputStream getAsZipStream() throws IOException {
		ByteArrayOutputStream memory = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(memory)) {
			addApplicationLogs(zip);
			addVehicleLogs(zip);
		}
		return new ByteArrayInputStream(memory.toByteArray());
	}
	
	private void addApplicationLogs(ZipOutputStream zip) throws IOException {
		for (LogFile logFile : Log4j.getLogFilesInChronologicOrder()) {
			zip.putNextEntry(new ZipEntry(logFile.getFile().getName()));
			IOUtils.copy(new FileInputStream(logFile.getFile()), zip);
		}
	}

	private void addVehicleLogs(ZipOutputStream zip) throws IOException {
		for (Vehicle vehicle : vehicleRepository.listAll())
			for(Task task : vehicle.getTasks(tasks)) {
				TaskLogFile logfile = new TaskLogFile(task);
				putFileIntoZip(logfile.getContent(), "vehicle-" + vehicle.id + "/" + logfile.getFilename(), zip);
			}
	}
	
	private void putFileIntoZip(String content, String filename, ZipOutputStream zip) throws IOException {
		zip.putNextEntry(new ZipEntry(filename));
		byte[] data = content.getBytes();
		zip.write(data, 0, data.length);
		zip.closeEntry();
	}

}
