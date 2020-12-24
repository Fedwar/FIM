package fleetmanagement.webserver;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.controllers.Redirect;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;

public class LicenceFilter implements ContainerRequestFilter {

    private final Licence licence;

    public LicenceFilter(Licence licence) {
        this.licence = licence;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {

        if (licence.isExpired()) {
            if (!isOnIgnoreList(request.getPath())
                    && !request.getPath().contains("licence")
                    && !request.getPath().contains("admin")
                    && !request.getPath().contains("about")
                    && !request.getPath().contains("login")
            ) {
                try {
                    request.setUris(request.getBaseUri(), new URI(request.getBaseUri().toString() + "admin"));
                    request.setMethod("GET");
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }

        return request;
    }

    private boolean isOnIgnoreList(String path) {
        String[] ignored = {".css", ".js", ".png", ".ico", ".woff", ".woff2", ".ttf", ".svg", ".eot"};
        return Stream.of(ignored).anyMatch(x -> path.endsWith(x));
    }

}
