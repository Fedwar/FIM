package fleetmanagement.backend.vehiclecommunication;

import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.MultiPartMediaTypes;
import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.tasks.LogEntry;
import fleetmanagement.backend.tasks.LogEntry.Severity;
import fleetmanagement.backend.tasks.TaskRepository;
import gsp.network.rest.RangeHeaderResponse;
import gsp.util.DoNotObfuscate;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class PackageResource {
	
	private final Package pkg;
	private final UUID taskId;
	private NotificationService notificationService;
	private final TaskRepository tasks;

	public PackageResource(Package pkg, TaskRepository tasks, UUID taskId, NotificationService notificationService) {
		this.pkg = pkg;
		this.tasks = tasks;
		this.taskId = taskId;
		this.notificationService = notificationService;
	}

	@GET
	@Path("meta")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public MetaFile buildMetaFile() throws Exception {
		tasks.update(taskId, task -> {
			if (task != null) 
				task.setClientDownloading();
		});
		
		MetaFile result = new MetaFile();
		Collection<File> allFiles = FileUtils.listFiles(pkg.path, null, true);

		for (File f : allFiles) {
			String relative = pkg.path.toPath().relativize(f.toPath()).toString();
			String normalized = relative.replace("\\", "/");
			try (InputStream is = new FileInputStream(f)) {
				result.files.add(new MetaItem(normalized, DigestUtils.sha256Hex(is), f.length()));
			}
		}
		return result;
	}
	
	@POST
	@Path("multi-file-download")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MultiPartMediaTypes.MULTIPART_MIXED + ";charset=utf-8")
	public Response deliverMultipleZipEntries(String request) throws Exception {
		List<String> files = Arrays.asList(request.split("\r\n"));
		MultiPart multiPart = new MultiPart();
		for (String filePath : files)
			multiPart.bodyPart(createMultipartEntry(filePath));
		return Response.status(Status.OK).entity(multiPart).type(MultiPartMediaTypes.MULTIPART_MIXED).build();
	}
	
	private BodyPart createMultipartEntry(String filePath) throws Exception {
		try {
			File file = new File(pkg.path, filePath);
			InputStream content = new FileInputStream(file);
	
			BodyPart result = new BodyPart(content, MediaType.APPLICATION_OCTET_STREAM_TYPE);
			result.getHeaders().add("Content-Length", Long.toString(file.length()));
			result.getHeaders().add("Content-Disposition", "attachment; filename=\"" + filePath + "\"");
			return result;
		}
		catch (Exception e) {
			logTaskError("Client requested invalid file: " + filePath + ", exception: " + e);
			throw e;
		}
	}
	
	@GET
	@Path("/{file: .+}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response deliverZipEntry(@PathParam("file") String file, @HeaderParam("Range") String range) throws Exception {
		try {
			InputStream content = new FileInputStream(new File(pkg.path, file));
		
			if (range != null)
				return RangeHeaderResponse.forRequest(content, range).type("application/octet-stream").build();
		
			return Response.ok(content).header("Content-Disposition", "attachment").build();
		}
		catch (Exception e) {
			logTaskError("Client requested invalid file: " + file + ", range " + range + ", exception: " + e);
			throw e;
		}
	}
		
	private void logTaskError(String message) {
		tasks.update(taskId, task -> {
			task.addLog(new LogEntry(Severity.ERROR, message));
		});
		notificationService.processEvent(Events.taskLogUpdated(tasks.tryFindById(taskId)));
	}

	@XmlRootElement
	public static class MetaFile {
		@XmlElement public List<MetaItem> files = new ArrayList<>(); 
	}
	
	@DoNotObfuscate
	public static class MetaItem {
		@XmlElement public String name;
		@XmlElement public String sha256;
		@XmlElement public long size;
		
		public MetaItem(String filename, String sha256, long size) {
			this.name = filename;
			this.sha256 = sha256;
			this.size = size;
		}
	}
}
