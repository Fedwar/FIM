package fleetmanagement.backend.groups;

import fleetmanagement.backend.repositories.Persistable;
import gsp.util.DoNotObfuscate;
import gsp.util.WrappedException;

import java.io.File;
import java.nio.file.WatchKey;
import java.util.UUID;

@DoNotObfuscate
public class Group implements Persistable<UUID> {
    public UUID id;
    public String name;
    public String dir;
    public WatchKey watchKey;
    public boolean isAutoSyncEnabled;

    public Group(String name, String dir, boolean isAutoSyncEnabled) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.dir = dir;
        this.isAutoSyncEnabled = isAutoSyncEnabled;
    }

    public Group(UUID id, String name, String dir, boolean isAutoSyncEnabled) {
        this.id = id;
        this.name = name;
        this.dir = dir;
        this.isAutoSyncEnabled = isAutoSyncEnabled;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Group clone() {
        try {
            return (Group) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new WrappedException(e);
        }
    }

}