package fleetmanagement.frontend.security.webserver;

import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.*;

import fleetmanagement.frontend.security.SecurityRole;

public class SecurityAnnotationProcessor implements ResourceFilterFactory {

	@Override
	public List<ResourceFilter> create(AbstractMethod method) {
		List<ResourceFilter> result = new ArrayList<>();
		if (!method.isAnnotationPresent(GuestAllowed.class) && !method.getResource().isAnnotationPresent(GuestAllowed.class))
			result.add(new EnsureAuthentication());

		if (method.isAnnotationPresent(UserRoleRequired.class) || method.getResource().isAnnotationPresent(UserRoleRequired.class))
			result.add(new EnsureUserHasRole(Arrays.asList(SecurityRole.Admin, SecurityRole.User, SecurityRole.Config)));

		if (method.isAnnotationPresent(ConfigRoleRequired.class) || method.getResource().isAnnotationPresent(ConfigRoleRequired.class))
			result.add(new EnsureUserHasRole(Arrays.asList(SecurityRole.Admin, SecurityRole.Config)));

		if (method.isAnnotationPresent(AdminRoleRequired.class) || method.getResource().isAnnotationPresent(AdminRoleRequired.class))
			result.add(new EnsureUserHasRole(Arrays.asList(SecurityRole.Admin)));

		return result;
	}
	
	private static class EnsureAuthentication implements ResourceFilter, ContainerRequestFilter {
		@Override
		public ContainerRequestFilter getRequestFilter() {
			return this;
		}

		@Override
		public ContainerResponseFilter getResponseFilter() {
			return null;
		}

		@Override
		public ContainerRequest filter(ContainerRequest request) {
			if (request.getSecurityContext().isSecure())
				return request;
			
			throw new WebApplicationException(Status.UNAUTHORIZED);
		}
	}
	
	private static class EnsureUserHasRole implements ResourceFilter, ContainerRequestFilter {
		
		private List<String> permittedRoles;

		public EnsureUserHasRole(List<SecurityRole> permittedRoles) {
			this.permittedRoles = permittedRoles.stream().map(r -> r.name()).collect(Collectors.toList());
		}
		
		@Override
		public ContainerRequestFilter getRequestFilter() {
			return this;
		}

		@Override
		public ContainerResponseFilter getResponseFilter() {
			return null;
		}

		@Override
		public ContainerRequest filter(ContainerRequest request) {
			for (String permittedRole : permittedRoles)
				if (request.getSecurityContext().isUserInRole(permittedRole))
					return request;
			
			throw new WebApplicationException(Status.FORBIDDEN);
		}
	}
}
