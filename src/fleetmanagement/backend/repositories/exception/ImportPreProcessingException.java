package fleetmanagement.backend.repositories.exception;

@SuppressWarnings("serial")
public class ImportPreProcessingException extends PackageImportException {
    public ImportPreProcessingException(String filename, Throwable e) {
        super(null, e, "Package preprocessing failed: " + filename);
    }
    public ImportPreProcessingException(String message) {
        super(null, message);
    }
}
