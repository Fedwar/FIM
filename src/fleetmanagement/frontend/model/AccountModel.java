package fleetmanagement.frontend.model;

import fleetmanagement.backend.accounts.Account;

import java.util.List;

public class AccountModel {
    public final String language;
    //public final UUID id;
    public final String name;
    public final String login;
    public final String email;
    public final String twitter;
    public final String website;
    public final String phone;
    public final String address;
    //public final String groups;
    public final List<String> availableLanguages;

    public AccountModel(Account account, List<String> languages) {
        name = account.name;
        language = account.language;
        address = account.address;
        phone = account.phone;
        email = account.email;
        login = account.login;
        twitter = account.twitter;
        website = account.website;
        availableLanguages = languages;
    }
}
