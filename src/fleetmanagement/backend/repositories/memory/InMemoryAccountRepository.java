package fleetmanagement.backend.repositories.memory;

import fleetmanagement.backend.accounts.Account;
import fleetmanagement.backend.repositories.disk.AccountXmlRepository;

import java.io.File;

public class InMemoryAccountRepository extends AccountXmlRepository {

    public InMemoryAccountRepository() {
        super((File)null);
    }

    @Override
    public void loadFromDisk() {}

    @Override
    protected File getDirectory(Account persistable) {
        return null;
    }

    @Override
    protected void persist(Account object) {}

}
