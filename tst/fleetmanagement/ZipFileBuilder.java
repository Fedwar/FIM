package fleetmanagement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("serial")
public class ZipFileBuilder {
    private File directory;
    private String fileName;
    private ArrayList<String> entries;

    public ZipFileBuilder(File directory, String fileName) {
        this.directory = directory;
        this.fileName = fileName;
        this.entries = new ArrayList<>();
    }

    public ZipFileBuilder addEntry(String filename) {
        entries.add(filename);
        return this;
    }

    public File create() throws IOException {
        File file = new File(directory, fileName);
        FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
        ZipOutputStream zos = new ZipOutputStream(fos);
        for (String entry : entries) {
            zos.putNextEntry(new ZipEntry(entry));
            zos.closeEntry();
        }
        zos.close();
        return file;
    }

}
