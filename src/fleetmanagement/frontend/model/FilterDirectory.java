package fleetmanagement.frontend.model;

import fleetmanagement.backend.vehiclecommunication.upload.filter.PathComposer;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilter;
import fleetmanagement.config.Licence;
import fleetmanagement.config.Settings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class FilterDirectory extends Admin {

    public String fullpath;
    public List<FileModel> files;
    private File root;
    private boolean filterHasAbsolutePath;

    public FilterDirectory(UploadFilter filter, String path, Licence licence, Settings settings) {
        super(licence);
        if (filter == null) {
            files = Collections.emptyList();
            fullpath = "";
        } else {
            String incomingFolderPath = settings.getIncomingFolderPath();
            root = filter.getAbsoluteCleanPath(new File(incomingFolderPath));
            File dir = new File(path);
            File filterDir = new File(PathComposer.getCleanPath(filter));
            filterHasAbsolutePath = filterDir.isAbsolute();

            if (filterDir.isAbsolute()) {
                fullpath = path;
            } else {
                fullpath = path;
                dir = new File(root.getParent(), path);
            }

            File[] listFiles = dir.listFiles();
            if (listFiles != null) {
                this.files = Arrays.stream(listFiles).map(FileModel::new).collect(Collectors.toList());
                Collections.sort(files);
            } else {
                this.files = new ArrayList<>();
            }

            if (!dir.equals(root)) {
                FileModel fileModel = new FileModel(dir.getParentFile());
                fileModel.name = "..";
                files.add(0, fileModel);
            }
        }
    }

    @Override
    public Licence getLicence() {
        return licence;
    }

    public class FileModel implements Comparable {
        public String name;
        public String length;
        public boolean isDirectory;
        public String date;
        public String path;

        public FileModel(File file) {
            name = file.getName();
            isDirectory = file.isDirectory();

            length = FileUtils.byteCountToDisplaySize(file.length());

            try {
                BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                LocalDateTime localDateTime = LocalDateTime.ofInstant(attr.creationTime().toInstant(), ZoneId.systemDefault());
                date = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(localDateTime);
            } catch (IOException ignored) {
            }

            if (filterHasAbsolutePath)
                path = file.toString();
            else
                path = file.toString().substring(root.getParent().length()+1);


        }

        @Override
        public int compareTo(Object o) {
            FileModel fileModel = (FileModel) o;
            return new CompareToBuilder()
                    .append(fileModel.isDirectory, this.isDirectory)
                    .append(this.name, fileModel.name)
                    .toComparison();

        }
    }
}
