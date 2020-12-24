package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.installations.PackageInstallation;
import fleetmanagement.backend.installations.PackageInstallationRepository;
import fleetmanagement.config.FimConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.UUID;

@Component
public class PackageInstallationSqlRepository extends SqlRepository<PackageInstallation, UUID> implements PackageInstallationRepository {

    private static final String DB_NAME = "packageInstallations.db";
    private static final Class<PackageInstallation> PERSIST_CLASS = PackageInstallation.class;

    @Autowired
    public PackageInstallationSqlRepository(FimConfig config) {
        this(config.getPackagesDirectory());
    }

    public PackageInstallationSqlRepository(File directory) {
        super(new HibernateSQLitePersistenceManager(directory, PERSIST_CLASS, DB_NAME));
        directory.mkdirs();
    }

    @Override
    protected Class<PackageInstallation> getEntityClass() {
        return PERSIST_CLASS;
    }

}
