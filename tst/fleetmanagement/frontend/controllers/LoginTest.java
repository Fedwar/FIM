package fleetmanagement.frontend.controllers;

import fleetmanagement.backend.accounts.Account;
import fleetmanagement.backend.accounts.AccountRepository;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.webserver.ModelAndView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginTest {
	private Login tested;
	@Mock private UserSession session;
	@Mock private AccountRepository accountRepository;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		when(session.getLocale()).thenReturn(Locale.ENGLISH);
		when(accountRepository.initAccount(session)).thenReturn(new Account());
		tested = new Login(session, accountRepository);
	}

	@Test
	public void logsIntoSessionAndRedirectsToDashboard() {
		when(session.login("user", "pass")).thenReturn(true);
		Response r = (Response)tested.processLoginRequest("user", "pass");

		verify(session).login("user", "pass");
		assertEquals("/dashboard", r.getMetadata().getFirst("Location").toString());
	}

	@Test
	public void initsUserProfileOnSuccesfulLogin() {
		when(session.login("user", "pass")).thenReturn(true);
		Response r = (Response)tested.processLoginRequest("user", "pass");

		verify(accountRepository).initAccount(session);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void showsErrorOnFailedLogin() {
		when(session.login("user", "pass")).thenReturn(false);
		
		ModelAndView<fleetmanagement.frontend.model.Login> result = (ModelAndView<fleetmanagement.frontend.model.Login>)tested.processLoginRequest("user", "pass");
		
		assertEquals("login.html", result.page);
		assertNotNull(result.viewmodel.error);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void returnsErrorWhenNotAllLoginFormFieldsAreFilled() {
		ModelAndView<fleetmanagement.frontend.model.Login> result = (ModelAndView<fleetmanagement.frontend.model.Login>)tested.processLoginRequest("user", null);

		assertEquals("login.html", result.page);
		assertNotNull(result.viewmodel.error);
	}
	
	@Test
	public void returnLoginPage() {
		ModelAndView<fleetmanagement.frontend.model.Login> result = tested.getLoginPage();

		assertEquals("login.html", result.page);
		assertNull(result.viewmodel.error);
	}
	
	@Test
	public void logsOutFromSession() {
		tested.logout();
		verify(session).logout();
	}
}
