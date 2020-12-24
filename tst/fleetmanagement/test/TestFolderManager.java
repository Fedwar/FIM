package fleetmanagement.test;

import org.junit.rules.ExternalResource;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestFolderManager extends ExternalResource {

    private String path;
    private File folder;
    private List<File> folderList = new ArrayList<>();

    public TestFolderManager(String path) {
        this.path = path;
    }

    protected void before() throws Throwable {
        this.create();
    }

    protected void after() {
        this.delete();
    }

    public void create() throws IOException {
        this.folder = new File(path);
        this.folder.mkdirs();
    }


    public File newFile(String fileName) throws IOException {
        File file = new File(this.getRoot(), fileName);
        if (!file.createNewFile()) {
            throw new IOException("a file with the name '" + fileName + "' already exists in the test folder");
        } else {
            return file;
        }
    }

    public void addToDeleteList(File file) throws Exception {
        if (file.getAbsolutePath().startsWith(folder.getAbsolutePath()))
            folderList.add(file);
        else
            throw new Exception("file is not in test folder");
    }

    public void addToDeleteList(String folderName) throws Exception {
        File file = this.getRoot();
        this.validateFolderName(folderName);
        file = new File(file, folderName);
        folderList.add(file);
    }

    public File newFolder(String folderName) throws IOException {
        File file = this.getRoot();
        this.validateFolderName(folderName);
        file = new File(file, folderName);
        file.mkdir();
        folderList.add(file);
        return file;
    }

    private void validateFolderName(String folderName) throws IOException {
        File tempFile = new File(folderName);
        if (tempFile.getParent() != null) {
            String errorMsg = "Folder name cannot consist of multiple path components separated by a file separator. Please use newFolder('MyParentFolder','MyFolder') to create hierarchies of folders";
            throw new IOException(errorMsg);
        }
    }

    public File getRoot() {
        if (this.folder == null) {
            throw new IllegalStateException("the temporary folder has not yet been created");
        } else {
            return this.folder;
        }
    }

    public void delete() {
        if (this.folderList.size() > 0) {
            folderList.stream().forEach(this::recursiveDelete);
        }
    }

    private void recursiveDelete(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            File[] arr$ = files;
            int len$ = files.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                File each = arr$[i$];
                this.recursiveDelete(each);
            }
        }

        file.delete();
    }
}
