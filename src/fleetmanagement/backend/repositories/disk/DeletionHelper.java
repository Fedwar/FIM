package fleetmanagement.backend.repositories.disk;

import gsp.util.WrappedException;

import java.io.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;

public class DeletionHelper {	
	private static final String DELETION_MARKER = ".todelete";

	public static void delete(File dir) {
		if (!FileUtils.deleteQuietly(dir))
			markForDeletion(dir);
	}
	
	public static void performPendingDeletes(File dir) {
		for (File deletionMarker : FileUtils.listFiles(dir, FileFilterUtils.nameFileFilter(DELETION_MARKER), TrueFileFilter.INSTANCE)) {
			FileUtils.deleteQuietly(deletionMarker.getParentFile());
		} 
	}
	
	private static void markForDeletion(File dir) {
		try {
			getDeletionMarker(dir).createNewFile();
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}
	
	private static File getDeletionMarker(File dir) {
		return new File(dir, DELETION_MARKER);
	}
}
