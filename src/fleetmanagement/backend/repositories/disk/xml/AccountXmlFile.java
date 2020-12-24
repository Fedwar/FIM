package fleetmanagement.backend.repositories.disk.xml;

import fleetmanagement.backend.accounts.Account;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.UUID;

public class AccountXmlFile implements XmlFile<Account> {

    private static final Logger logger = Logger.getLogger(AccountXmlFile.class);
    private static final XmlSerializer serializer = new XmlSerializer(AccountXml.class);
    private static final String fileName = "account.xml";
    private final File file;

    public AccountXmlFile(File directory) {
        this.file = new File(directory, fileName);
    }

    @Override
    public File file() {
        return file;
    }

    public void delete() {
        file.delete();
    }

    public boolean exists() {
        return file.exists();
    }

    public Account load() {
        try {
            if (exists()) {
                AccountXml accountXml = (AccountXml)serializer.load(file);
                return accountXml.toAccount();
            }
        } catch (Exception e) {
            logger.error("Account in " + file.getParent() + " seems broken.", e);
        }
        return null;
    }

    public void save(Account account) {
        createParentDirectoryIfRequired();
        AccountXml meta = new AccountXml(account);
        serializer.save(meta, file);
    }

    private void createParentDirectoryIfRequired() {
        File parent = file.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
    }

    @XmlRootElement(name="account")
    @DoNotObfuscate
    private static class AccountXml {
        @XmlAttribute public UUID id;
        @XmlAttribute public String name;
        @XmlAttribute public String login;
        @XmlAttribute public String email;
        @XmlAttribute public String twitter;
        @XmlAttribute public String website;
        @XmlAttribute public String phone;
        @XmlAttribute public String address;
        @XmlAttribute public String groups;
        @XmlAttribute public String language;

        public AccountXml() {
        }

        public AccountXml(Account o) {
            id = o.id;
            name = o.name;
            login = o.login;
            email = o.email;
            twitter = o.twitter;
            website = o.website;
            phone = o.phone;
            address = o.address;
            groups = o.groups;
            language = o.language;
        }

        public Account toAccount() {
            return new Account(id,name,login,email,twitter,website,phone,address,groups,language);
        }
    }

    public File getFile() {
        return  file;
    }
}
