package fleetmanagement.frontend.controllers;

import javax.ws.rs.*;

import fleetmanagement.backend.accounts.Account;
import fleetmanagement.backend.accounts.AccountRepository;
import fleetmanagement.backend.packages.ActivityLog;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.security.webserver.GuestAllowed;
import fleetmanagement.frontend.webserver.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@GuestAllowed
@Path("login")
@Component
public class Login extends FrontendController {

	private final AccountRepository accountRepository;

	@Autowired
	public Login(UserSession request, AccountRepository accountRepository) {
		super(request);
		this.accountRepository = accountRepository;
	}

	@GET
	public ModelAndView<fleetmanagement.frontend.model.Login> getLoginPage() {
		fleetmanagement.frontend.model.Login vm = new fleetmanagement.frontend.model.Login();
		return new ModelAndView<>("login.html", vm);
	}
	
	@POST
	public Object processLoginRequest(@FormParam("username") String user, @FormParam("password") String password) {
		if (user == null || password == null) {
			return loginError(i18n("login_form_not_filled"));
		}
		
		if (session.login(user, password)) {
			Account account = accountRepository.initAccount(session);
			session.setSelectedLanguage(account.language);
			ActivityLog.userMessage(ActivityLog.Operations.USER_LOGIN,
					session.getUsername());
			return Redirect.to("/dashboard");
		} else
			return loginError(i18n("login_failed"));
	}
	
	@Path("logout")
	@GET
	public ModelAndView<fleetmanagement.frontend.model.Login> logout() {
		session.logout();
		return loginError(i18n("login_loggedout"));
	}
	

	private ModelAndView<fleetmanagement.frontend.model.Login> loginError(String error) {
		fleetmanagement.frontend.model.Login vm = new fleetmanagement.frontend.model.Login();
		vm.error = error;
		return new ModelAndView<>("login.html", vm);
	}

}
