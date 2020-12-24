package fleetmanagement.backend.vehiclecommunication;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.ActivityLog;
import fleetmanagement.backend.vehiclecommunication.upload.filter.FilterSequenceRepository;
import fleetmanagement.backend.vehiclecommunication.upload.filter.PathComposer;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilter;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilterSequence;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Settings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

import static fleetmanagement.backend.vehiclecommunication.upload.filter.FilterType.AD_FILTER_TYPE;

@Component
@Path("upload/filter")
public class FilteredUploadResource {

    private static final Logger logger = Logger.getLogger(FilteredUploadResource.class);

    @Autowired
    private FilterSequenceRepository filters;
    @Autowired
    private VehicleRepository vehicles;
    @Autowired
    private GroupRepository groups;
    @Autowired
    private Settings settings;

    public FilteredUploadResource() {
    }

    FilteredUploadResource(VehicleRepository vehicles, FilterSequenceRepository filters, GroupRepository groups) {
        this.vehicles = vehicles;
        this.filters = filters;
        this.groups = groups;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void uploadFile(@QueryParam("uic") String vehicleUic, @FormDataParam("file") InputStream fileContent, @FormDataParam("file") FormDataContentDisposition contentDisposition)
            throws IOException {
        byte[] data = receive(fileContent);
        String filename = contentDisposition.getFileName();
        Vehicle sender = vehicles.tryFindByUIC(vehicleUic);

        if (sender != null) {
            logger.info("Received file from vehicle " + vehicleUic + ": " + filename);
            onFileReceived(sender, filename, data);
        } else {
            logger.warn("Received file from unknown vehicle " + vehicleUic + ": " + filename);
        }
    }

    long calcAge(int days) {
        return (long) days * 24 * 60 * 60 * 1000;
    }

    private void deleteObsoleteFiles(File incomingFolder, UploadFilter filter, Vehicle v, Group g) {
        if (filter == null) {
            logger.warn("Filter is NULL in deleteObsoleteFiles()! Please report the case to developers.");
            return;
        }
        if (!filter.delete) {
            logger.debug("Delete is disabled for filter " + filter.description);
            return;
        }

        File dir = PathComposer.composeFromRoot(filter.dir, v, g, incomingFolder);

        logger.debug("Deleting obsolete files in " + dir.getAbsolutePath());
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            long currentTime = new Date().getTime();
            long age = calcAge(filter.deleteDays);
            for (File f : directoryListing) {
                logger.debug("Checking file " + f.getName());
                if (f.isDirectory()) continue;
                if (currentTime - f.lastModified() > age) {
                    if (f.delete()) {
                        ActivityLog.vehicleFilteredInfoReceived(ActivityLog.Operations.OBSOLETE_FILE_DELETED, v, g, f.getName(), dir.getAbsolutePath());
                    } else {
                        logger.warn("Can't delete obsolete file! File is " + f.getName() + ". Directory is " + dir.getAbsolutePath() + ". Please report the case to developers.");
                    }
                } else {
                    logger.debug("File is not old, keeping it.");
                }
            }
        } else {
            logger.debug("No files in the filter directory");
        }
    }

    public void onFileReceived(Vehicle sender, String fileName, byte[] data) {
        try {
            Group group = null;
            if (sender.getGroupId() != null && !sender.getGroupId().isEmpty())
                group = groups.tryFindById(UUID.fromString(sender.getGroupId()));
            UploadFilterSequence filterSequence = filters.findByType(AD_FILTER_TYPE);
            UploadFilter matchFilter = filterSequence.match(sender, group, fileName);
            File filtersRootDirectory = getFiltersRootDirectory();

            if (matchFilter != null) {
                File filterDir = PathComposer.composeFromRoot(matchFilter.dir, sender, group, filtersRootDirectory);
                File destFile = new File(filterDir, fileName);
                deleteObsoleteFiles(filtersRootDirectory, matchFilter, sender, group);
                File saved = writeFile(destFile, data);
                ActivityLog.vehicleFilteredInfoReceived(ActivityLog.Operations.AD_INFO_RECEIVED, sender, group, fileName, destFile.getAbsolutePath());

                filters.update(
                        filterSequence.id,
                        fs -> fs.notViewedFiles.add(new File(matchFilter.dir).isAbsolute()
                                ? saved.toString()
                                : filtersRootDirectory.getAbsoluteFile().toPath().relativize(saved.toPath()).toString())
                );
            } else {
                File unfilteredFolder = new File(filtersRootDirectory, "Unfiltered");
                ZonedDateTime now = ZonedDateTime.now();
                fileName = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + File.separator + sender.getName() + " " + now.format(DateTimeFormatter.ofPattern("HH.mm")) + " " + fileName;
                File unfilteredFile = new File(unfilteredFolder, fileName);
                writeFile(unfilteredFile, data);
                logger.warn("Uploaded file " + fileName + " doesn't match any filters, storing as " + unfilteredFile.getAbsolutePath() + ".");
                ActivityLog.vehicleFilteredInfoReceived(ActivityLog.Operations.AD_INFO_RECEIVED, sender, group, fileName, unfilteredFile.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.warn("Error receiving file: " + fileName, e);
        }
    }

    public File getFiltersRootDirectory() {
        String incomingFolderPath = settings.getIncomingFolderPath();
        return new File(incomingFolderPath);
    }

    private File writeFile(File file, byte[] data) throws IOException {
        File saveFile = findFileName(file);
        FileUtils.writeByteArrayToFile(saveFile, data);
        return saveFile;
    }

    private static File findFileName(File file) {
        String extension = FilenameUtils.getExtension(file.getName());
        String baseName = FilenameUtils.getBaseName(file.getName());
        String dir = file.getParentFile().getAbsolutePath();

        java.nio.file.Path ret = Paths.get(dir, String.format("%s.%s", baseName, extension));
        if (!Files.exists(ret))
            return ret.toFile();

        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            ret = Paths.get(dir, String.format("%s_%d.%s", baseName, i, extension));
            if (!Files.exists(ret))
                return ret.toFile();
        }
        throw new IllegalStateException("File count limit exceeded");
    }

    private byte[] receive(InputStream fileContent) throws IOException {
        return IOUtils.toByteArray(fileContent);
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }
}
