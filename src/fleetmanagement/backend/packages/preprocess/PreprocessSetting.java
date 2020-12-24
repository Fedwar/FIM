package fleetmanagement.backend.packages.preprocess;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.repositories.Persistable;
import gsp.util.WrappedException;

import java.util.UUID;

public class PreprocessSetting implements Persistable<UUID> {

    public final UUID id;
    public final PackageType packageType;
    public final String command;
    public final String options;
    public final String fileNamePattern;

    public PreprocessSetting(UUID id, PackageType packageType, String command, String options, String fileNamePattern) {
        this.id = id;
        this.packageType = packageType;
        this.command = command;
        this.options = options;
        this.fileNamePattern = fileNamePattern;
    }

    public PreprocessSetting(PackageType packageType, String command, String options, String fileNamePattern) {
        this(UUID.randomUUID(), packageType, command, options, fileNamePattern);
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public PreprocessSetting clone() {
        try {
            return (PreprocessSetting) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new WrappedException(e);
        }
    }
}
