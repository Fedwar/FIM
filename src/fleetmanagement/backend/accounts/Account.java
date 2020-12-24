package fleetmanagement.backend.accounts;

import fleetmanagement.backend.repositories.Persistable;
import fleetmanagement.frontend.UserSession;
import gsp.util.DoNotObfuscate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@DoNotObfuscate
public class Account implements Persistable<UUID> {
    public final UUID id;
    public String name;
    public String login;
    public String email;
    public String twitter;
    public String website;
    public String phone;
    public String address;
    public String groups;
    public String language;

    public Account(UserSession session) {
        id = UUID.randomUUID();
        this.login = session.getUsername();
        this.language = session.getLocale().getLanguage();
    }

    public Account(UUID id, String name, String login, String email, String twitter, String website, String phone, String address, String groups, String language) {
        this.id = id;
        this.name = name;
        this.login = login;
        this.email = email;
        this.twitter = twitter;
        this.website = website;
        this.phone = phone;
        this.address = address;
        this.groups = groups;
        this.language = language;
    }

    public Account() {
        id = UUID.randomUUID();
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Account clone() {
        try {
            return (Account)super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTwitter() {
        return twitter;
    }

    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGroups() {
        return groups;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }

}
