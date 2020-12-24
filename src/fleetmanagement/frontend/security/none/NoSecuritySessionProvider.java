package fleetmanagement.frontend.security.none;

import java.util.HashMap;

import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.security.webserver.ThreadLocalSessionFilter.SessionProvider;
import gsp.util.MonotonicClock;

public class NoSecuritySessionProvider implements SessionProvider {
	
	private static final long SESSION_EXPIRY_TIMEOUT_MSECS = 30 * 60 * 1000;

	private final HashMap<String, NoSecuritySession> sessions = new HashMap<>();
	private final MonotonicClock clock;
	private long nextSessionCleanup = 0;
	
	public NoSecuritySessionProvider(MonotonicClock clock) {
		this.clock = clock;
	}

	@Override
	public UserSession get(String sessionId) {
		cleanupExpiredSessions();
		
		if (sessionId == null || !sessions.containsKey(sessionId)) {
			return createNewSession();
		}
		
		return sessions.get(sessionId);
	}

	private UserSession createNewSession() {
		NoSecuritySession session = new NoSecuritySession(clock.getTickCount());
		sessions.put(session.getId(), session);
		return session;
	}

	private void cleanupExpiredSessions() {
		long now = clock.getTickCount();
		if (now > nextSessionCleanup) {
			sessions.entrySet().removeIf(x ->x.getValue().lastActivity + SESSION_EXPIRY_TIMEOUT_MSECS < now);
			scheduleNextSessionCleanup();
		}
	}	
	
	private void scheduleNextSessionCleanup() {
		this.nextSessionCleanup = clock.getTickCount() + SESSION_EXPIRY_TIMEOUT_MSECS;
	}
}
