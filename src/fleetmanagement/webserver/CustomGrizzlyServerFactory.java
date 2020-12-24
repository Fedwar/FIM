package fleetmanagement.webserver;

import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import java.io.IOException;
import java.net.URI;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

//copy of GrizzlyServerFactory
// creates http server without start
public final class CustomGrizzlyServerFactory {
    public static final String FEATURE_ALLOW_ENCODED_SLASH = "com.sun.jersey.api.container.grizzly.AllowEncodedSlashFeature";

    public static HttpServer createHttpServer(String u) throws IOException, IllegalArgumentException, NullPointerException {
        if (u == null) {
            throw new NullPointerException("The URI must not be null");
        } else {
            return createHttpServer(URI.create(u));
        }
    }

    public static HttpServer createHttpServer(String u, ResourceConfig rc) throws IOException, IllegalArgumentException, NullPointerException {
        if (u == null) {
            throw new NullPointerException("The URI must not be null");
        } else {
            return createHttpServer(URI.create(u), rc);
        }
    }

    public static HttpServer createHttpServer(String u, ResourceConfig rc, IoCComponentProviderFactory factory) throws IOException, IllegalArgumentException, NullPointerException {
        if (u == null) {
            throw new NullPointerException("The URI must not be null");
        } else {
            return createHttpServer(URI.create(u), rc, factory);
        }
    }

    public static HttpServer createHttpServer(URI u) throws IOException, IllegalArgumentException, NullPointerException {
        HttpHandler handler = (HttpHandler)ContainerFactory.createContainer(HttpHandler.class);
        return createHttpServer(u, handler);
    }

    public static HttpServer createHttpServer(URI u, ResourceConfig rc) throws IOException, IllegalArgumentException, NullPointerException {
        HttpHandler handler = (HttpHandler)ContainerFactory.createContainer(HttpHandler.class, rc);
        return createHttpServer(u, handler);
    }

    public static HttpServer createHttpServer(URI u, ResourceConfig rc, IoCComponentProviderFactory factory) throws IOException, IllegalArgumentException, NullPointerException {
        HttpHandler processor = (HttpHandler)ContainerFactory.createContainer(HttpHandler.class, rc, factory);
        return createHttpServer(u, processor);
    }

    public static HttpServer createHttpServer(URI u, HttpHandler handler, boolean secure, SSLEngineConfigurator sslEngineConfigurator) throws IOException, IllegalArgumentException, NullPointerException {
        if (u == null) {
            throw new NullPointerException("The URI must not be null");
        } else {
            String scheme = u.getScheme();
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                throw new IllegalArgumentException("The URI scheme, of the URI " + u + ", must be equal (ignoring case) to 'http' or 'https'");
            } else {
                String host = u.getHost() == null ? "0.0.0.0" : u.getHost();
                int port = u.getPort() == -1 ? 80 : u.getPort();
                HttpServer server = new HttpServer();
                NetworkListener listener = new NetworkListener("grizzly", host, port);
                listener.setSecure(secure);
                if (sslEngineConfigurator != null) {
                    listener.setSSLEngineConfig(sslEngineConfigurator);
                }

                server.addListener(listener);
                ServerConfiguration config = server.getServerConfiguration();
                if (handler != null) {
                    config.addHttpHandler(handler, new String[]{u.getPath()});
                }

                return server;
            }
        }
    }

    public static HttpServer createHttpServer(URI u, HttpHandler handler, boolean secure) throws IOException, IllegalArgumentException, NullPointerException {
        return createHttpServer(u, handler, secure, (SSLEngineConfigurator)null);
    }

    public static HttpServer createHttpServer(URI u, HttpHandler handler) throws IOException, IllegalArgumentException, NullPointerException {
        return createHttpServer(u, handler, false, (SSLEngineConfigurator)null);
    }

}
