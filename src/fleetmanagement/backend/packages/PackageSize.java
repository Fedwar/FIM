package fleetmanagement.backend.packages;

import java.io.*;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;

public class PackageSize {
	public final int files;
	public final int bytes;
	
	public PackageSize(File directory) throws IOException {
		this.files = countFilesIn(directory);
		this.bytes = (int)FileUtils.sizeOfDirectory(directory);
	}
	
	public PackageSize(int files, int bytes) {
		this.files = files;
		this.bytes = bytes;
	}
	
	private static int countFilesIn(File dir) throws IOException {
		int result = 0;
		Iterator<File> it = FileUtils.iterateFiles(dir, null, true);
		while (it.hasNext()) {
			it.next();
			result++;
		}
		
		return result;
	}
	
	@Override
	public int hashCode() {
		return bytes;
	}
	
	@Override
	public boolean equals(Object obj) {
		PackageSize other = (PackageSize)obj;
		return other.files == this.files && other.bytes == this.bytes;
	}
	
	@Override
	public String toString() {
		return String.format("%d files, %d bytes", files, bytes);
	}
}
