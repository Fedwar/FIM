package fleetmanagement.frontend;

import gsp.configuration.LocalFiles;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.zip.*;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.sun.jersey.api.NotFoundException;

public class WebFiles {

    private static final String ROOT = "frontend";
    private static final Logger logger = Logger.getLogger(WebFiles.class);

    public static InputStream open(String path) {
        try {
            File resource = LocalFiles.find(ROOT + "/" + path);
            return new FileInputStream(resource);
        } catch (Exception e) {
            throw new NotFoundException("Not found: " + path);
        }
    }

    public static File file(String path) {
        File resource = LocalFiles.find(ROOT + "/" + path);
        return resource;
    }

    public static void init() throws IOException {
        if (isRunningFromJar()) {
            logger.info("Extracting frontend files");
            extractFromJar("/" + ROOT, new File(ROOT));
        }
    }

    public static void shutdown() throws IOException {
        if (isRunningFromJar()) {
            FileUtils.deleteDirectory(new File(ROOT));
        }
    }

    private static boolean isRunningFromJar() {
        URL currentClass = getUrlOfCurrentClass();
        return currentClass.getProtocol().equals("jar");
    }

    private static URL getUrlOfCurrentClass() {
        return WebFiles.class.getResource(WebFiles.class.getSimpleName() + ".class");
    }

    private static void extractFromJar(String sourceDirectory, File destDirectory) throws IOException {
        FileUtils.deleteDirectory(destDirectory);
        JarURLConnection jarConnection = (JarURLConnection) getUrlOfCurrentClass().openConnection();
        try (ZipFile jar = jarConnection.getJarFile()) {
            String path = sourceDirectory.substring(1);
            Enumeration<? extends ZipEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(path))
                    continue;

                String entryTail = name.substring(path.length());
                File f = new File(destDirectory, entryTail);
                if (entry.isDirectory()) {
                    if (!f.mkdir())
                        throw new RuntimeException("Unable to create directory " + f);
                } else {
                    try (InputStream is = jar.getInputStream(entry)) {
                        FileUtils.copyInputStreamToFile(is, f);
                    }
                }
            }
        }
    }

}
