package fleetmanagement.backend.accounts;

import fleetmanagement.backend.repositories.Repository;
import fleetmanagement.frontend.UserSession;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public interface AccountRepository extends Repository<Account, UUID> {

    Account findByLogin(String login);

    Account initAccount(UserSession session);

    void setAccountPhoto(String login, String fileName, InputStream data) throws IOException;

    File getAccountPhoto(String login) throws IOException;
}
