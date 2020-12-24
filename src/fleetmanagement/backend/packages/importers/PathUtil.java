package fleetmanagement.backend.packages.importers;

import gsp.util.WrappedException;

import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;

public class PathUtil {
	
	public static File findCaseInsensitive(File root, String subpath) {
		Path path = tryFindCaseInsensitive(root.toPath(), subpath);
		return path.toFile();
	}
	
	private static Path tryFindCaseInsensitive(Path root, String subpath) {
		try {
			return findCaseInsensitive(root, subpath);
		}
		catch (FileNotFoundException e) {
			return root.resolve(subpath);
		}
		catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	private static Path findCaseInsensitive(Path root, String subpath) throws IOException {
		Path resolved = root;
		for (Path component : Paths.get(subpath)) {
			try (Stream<Path> files = Files.list(resolved)) {
				resolved = files.filter(x -> x.getFileName().toString().equalsIgnoreCase(component.toString()))
								.findAny().orElseThrow(() -> new FileNotFoundException(root.resolve(subpath).toString()));				
			}
		}
		
		return resolved;
	}
}
