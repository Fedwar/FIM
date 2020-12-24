package fleetmanagement;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

@SuppressWarnings("serial")
public class TempFile extends File implements Closeable {

    public TempFile() {
        super(prepareTempFolder());
    }

    public TempFile(File file, String path) {
        super(file, path);
    }

    public static TempFile create() {
        return new TempFile(prepareTempFolder());
    }

    private static String prepareTempFolder() {
        try {
            File temp = File.createTempFile("unittest-", "");
            temp.delete();
            temp.deleteOnExit();
            temp.mkdir();

            return temp.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TempFile(String name) {
        super(name);
    }

    public TempFile append(String path) {
        return new TempFile(this, path);
    }

    public TempFile newFolder(String path) {
        TempFile temporaryDirectoryRule = new TempFile(this, path);
        temporaryDirectoryRule.mkdirs();
        return temporaryDirectoryRule;
    }

    public TempFile addFolder(String path) {
        newFolder(path);
        return this;
    }

    public void clean() throws IOException {
        FileUtils.cleanDirectory(this);
    }

    public File newFile(String fileName) throws IOException {
        File file = new File(this, fileName);
        if (!file.createNewFile()) {
            throw new IOException("a file with the name '" + fileName + "' already exists in the test folder");
        } else {
            return file;
        }
    }

    public TempFile addFile(String fileName) throws IOException {
        newFile(fileName);
        return this;
    }

    @Override
    public boolean delete() {
        if (!exists())
            return true;

        FileUtils.deleteQuietly(new File(getPath()));
        return true;
    }


    @Override
    public void close() throws IOException {
        delete();
    }

}
