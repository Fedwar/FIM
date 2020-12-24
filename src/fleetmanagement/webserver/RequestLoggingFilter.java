package fleetmanagement.webserver;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.apache.log4j.Logger;

import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.SecurityContext;
import java.util.stream.Stream;

public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
	
	private static final Logger logger = Logger.getLogger(RequestLoggingFilter.class);

	@Override
	public ContainerRequest filter(ContainerRequest request) {
		if (!isOnIgnoreList(request.getPath())) {
			if (request.getPath().contains("ajax")) {
				logger.debug(getRequestLogString(request));
			} else {
				logger.info(getRequestLogString(request));
			}
		}
		return request;
	}

	private String getRequestLogString(ContainerRequest request) {
		return String.format("[%s] %s %s", getOptionalUser(request), request.getMethod(), request.getRequestUri());
	}

	private String getOptionalUser(ContainerRequest request) {
		SecurityContext ctx = request.getSecurityContext();
		if (ctx == null || ctx.getUserPrincipal() == null)
			return "anonymous";
		
		return ctx.getUserPrincipal().getName();
	}

	@Override
	public ContainerResponse filter(ContainerRequest req, ContainerResponse res) {
		if (isErrorResponse(res))
			logger.error(String.format("%d -> %s", res.getStatus(), getRequestLogString(req)));
		
		return res;
	}

	private boolean isErrorResponse(ContainerResponse res) { 
		return res.getStatusType().getFamily() != Family.SUCCESSFUL && res.getStatusType().getFamily() != Family.REDIRECTION;
	}

	private boolean isOnIgnoreList(String path) {
		String[] ignored = { ".css", ".js", ".png", ".ico", ".woff", ".woff2", ".ttf", ".svg", ".eot" };
		return Stream.of(ignored).anyMatch(x -> path.endsWith(x));
	}
}
