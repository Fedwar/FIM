package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.accounts.Account;
import fleetmanagement.backend.accounts.AccountRepository;
import fleetmanagement.backend.repositories.disk.xml.AccountXmlFile;
import fleetmanagement.backend.repositories.disk.xml.XmlFile;
import fleetmanagement.config.FimConfig;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.WebFiles;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Component
public class AccountXmlRepository extends GenericOnDiskRepository<Account, UUID>  implements AccountRepository {

    private static final Logger logger = Logger.getLogger(AccountXmlRepository.class);

    @Autowired
    public AccountXmlRepository(FimConfig config) {
        super(config.getAccountsDirectory());
    }

    public AccountXmlRepository(File directory) {
        super(directory);
    }

    @Override
    @PostConstruct
    public void loadFromDisk() {
        logger.debug("Loading from disk: accounts");
        super.loadFromDisk();
    }

    @Override
    protected XmlFile<Account> getXmlFile(File dir) {
        return new AccountXmlFile(dir);
    }

    @Override
    public Account findByLogin(String login) {
        return persistables.stream()
                .filter(x -> x.login.equals(login))
                .findFirst().orElse(null);
    }

    @Override
    public Account initAccount(UserSession session) {
        Account account = findByLogin(session.getUsername());
        if (account == null) {
            account = new Account(session) ;
            insert(account);
        }
        return account;
    }

    @Override
    public void setAccountPhoto(String username, String fileName, InputStream data) throws IOException {
        Account account = findByLogin(username);
        File accountDir = getDirectory(account);
        if (fileName == null) {
            File[] files = accountDir.listFiles((FileFilter) new WildcardFileFilter("photo.*"));
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        } else {
            FileUtils.copyInputStreamToFile(data, new File(accountDir, "photo." + FilenameUtils.getExtension(fileName)));
        }
    }

    @Override
    public File getAccountPhoto(String username) throws IOException {
        Account account = findByLogin(username);
        File accountDir = getDirectory(account);
        File[] files = accountDir.listFiles((FileFilter) new WildcardFileFilter("photo.*"));
        if (files != null && files.length > 0) {
            return files[0];
        }
        return WebFiles.file("img/blank_portrait.png");
    }

}
