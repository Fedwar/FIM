package fleetmanagement.backend.vehicles;

import fleetmanagement.backend.packages.PackageType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class VehicleVersions {

    private final Set<Versioned> versions = new HashSet<>();

    public static class Versioned {

        public final PackageType type;
        public final String version;
        public final Integer slot;
        public final String validityBegin;
        public final String validityEnd;
        public final Boolean active;

        public Versioned(PackageType type, String version) {
            this(type, 0, version);
        }

        public Versioned(PackageType type, Integer slot, String version) {
            this(type, slot, version, null, null, false);
        }

        public Versioned(PackageType type, Integer slot, String version, String validityBegin, String validityEnd, Boolean active) {
            this.type = type;
            this.slot = defaultIfNull(slot, Integer.valueOf(0));
            this.version = version;
            this.validityBegin = validityBegin;
            this.validityEnd = validityEnd;
            this.active = active;
        }

        public PackageType getType() {
            return type;
        }

        public Integer getSlot() {
            return slot;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Versioned versioned = (Versioned) o;
            return type == versioned.type &&
                    Objects.equals(slot, versioned.slot);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, slot);
        }
    }

    public String getDataSupplyVersion(int slot) {
        Versioned versioned = get(PackageType.DataSupply, slot);
        return versioned == null ? null : versioned.version;
    }

    public String getVersion(PackageType packageType, int slot) {
        Versioned versioned = get(packageType, slot);
        return versioned == null ? null : versioned.version;
    }



    public Set<Versioned> getAll() {
        return new HashSet(versions);
    }

    public Set<Versioned> getAll(PackageType type) {
        return versions.stream()
                .filter(versioned -> versioned.type == type)
                .collect(Collectors.toSet());
    }

    public Set<String> getAllVersionsByType(PackageType type) {
        return versions.stream()
                .filter(versioned -> versioned.type == type)
                .map(versioned -> versioned.version)
                .collect(Collectors.toSet());
    }

    public void setDataSupplyVersion(int slot, String version) {
        set(PackageType.DataSupply, version, slot, null, null, null);
    }

    public void add(Versioned component) {
        versions.add(component);
    }

    public Versioned get(PackageType type, Integer slot) {
        Integer finalSlot = defaultIfNull(slot, Integer.valueOf(0));
        return versions.stream().filter(v -> v.type == type && v.slot.equals(finalSlot)).findAny().orElse(null);
    }

    public Versioned get(PackageType type) {
        Versioned versioned = get(type, 0);
        return versioned;
    }

    public void set(PackageType type, String version) {
        set(type, version, 0, null, null, null);
    }

    public void set(PackageType type, String version, Integer slot, String validityBegin, String validityEnd, Boolean active) {
        Integer finalSlot = defaultIfNull(slot, Integer.valueOf(0));;
        versions.removeIf(v -> v.type == type && v.slot.equals(finalSlot));
        if (version != null) {
            Versioned versioned = new Versioned(type, finalSlot, version, validityBegin, validityEnd, active);
            versions.add(versioned);
        }
    }

    public void removeAll(PackageType type) {
        versions.removeIf(v -> v.type == type);
    }

}
