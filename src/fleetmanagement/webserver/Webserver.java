package fleetmanagement.webserver;

import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.container.filter.GZIPContentEncodingFilter;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.HttpServerProbe;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.threadpool.GrizzlyExecutorService;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class Webserver {
    private final Logger logger = Logger.getLogger(Webserver.class);
    private final ResourceConfig resources = new DefaultResourceConfig(MultiPartWriter.class);
    private final int port;
    private HttpServer http;
    private boolean enableTls;
    private File certDirectory;

    public HttpServer getHttp() {
        return http;
    }

    public Webserver(int port, boolean enableTls) {
        this(port, enableTls, new File("./certificates"));
    }

    public Webserver(int port, boolean enableTls, File certDirectory) {
        addRequestFilter(GZIPContentEncodingFilter.class);
        addResponseFilter(RequestLoggingFilter.class);
        addResponseFilter(GZIPContentEncodingFilter.class);
        this.resources.getSingletons().add(new GsonJerseyProvider());
        this.port = port;
        this.enableTls = enableTls;
        this.certDirectory = certDirectory;

        addResource(new MonitoringResource());
        addResource(new MonitoringResource.HealthRedirectResource());
    }

    public int getPort() {
        return port;
    }

    public void addResource(Object resource) {
        resources.getSingletons().add(resource);
    }

    public <T> void addMessageBodyWriter(MessageBodyWriter<T> writer) {
        resources.getSingletons().add(writer);
    }

    public <T extends Throwable> void addExceptionMapper(ExceptionMapper<T> mapper) {
        resources.getSingletons().add(mapper);
    }

    @SuppressWarnings("unchecked")
    public void addResourceFilterFactory(ResourceFilterFactory f) {
        resources.getResourceFilterFactories().add(f);
    }

    @SuppressWarnings("unchecked")
    public void addRequestFilter(Class<? extends ContainerRequestFilter> filter) {
        resources.getContainerRequestFilters().add(filter);
    }

    @SuppressWarnings("unchecked")
    public void addResponseFilter(Class<? extends ContainerResponseFilter> filter) {
        resources.getContainerResponseFilters().add(filter);
    }

    @SuppressWarnings("unchecked")
    public void addRequestFilter(ContainerRequestFilter ctx) {
        resources.getContainerRequestFilters().add(ctx);
    }

    @SuppressWarnings("unchecked")
    public void addResponseFilter(ContainerResponseFilter ctx) {
        resources.getContainerResponseFilters().add(ctx);
    }

    public void start() throws Exception {
        System.setProperty("enableLogging", "true");
        http = enableTls ? createHttpsServer(resources) : createHttpServer(resources);
        configure();
        http.start();
        ensureParallelRequestsDontDegradePerformance(http);
    }

    private void configure() {
        //adds remoteAddr to header, so we can access it in Jersey
        http.getServerConfiguration().getMonitoringConfig().getWebServerConfig()
                .addProbes(new HttpServerProbe.Adapter() {
                    @Override
                    public void onRequestReceiveEvent(HttpServerFilter filter,
                                                      Connection connection,
                                                      Request request) {
                        if (request.getRequest().getMethod().getMethodString().equals("POST"))
                            request.getRequest().setHeader("remoteAddr", request.getRemoteAddr());
                    }
                });
    }

    public void stop() {
        if (http != null) {
            http.stop();
            http = null;
        }
    }

    private HttpServer createHttpsServer(ResourceConfig rc) throws IOException, URISyntaxException {
        HttpHandler h = ContainerFactory.createContainer(HttpHandler.class, rc);
        SSLContextConfigurator sslContext = new SSLContextConfigurator();
        File keyStoreFile = new File(certDirectory, "keystore.jks");
        sslContext.setKeyStoreFile(keyStoreFile.getAbsolutePath()); // contains server key
        sslContext.setKeyStorePass("hd84w19!");
        File trustStoreFile = new File(certDirectory, "truststore.jks");
        sslContext.setTrustStoreFile(trustStoreFile.getAbsolutePath());

        SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslContext);
        sslEngineConfigurator.setNeedClientAuth(true);
        sslEngineConfigurator.setClientMode(false);

        return CustomGrizzlyServerFactory.createHttpServer(new URI("https://0.0.0.0:" + port), h, true, sslEngineConfigurator);
    }

    private HttpServer createHttpServer(ResourceConfig rc) throws IOException {
        return CustomGrizzlyServerFactory.createHttpServer("http://0.0.0.0:" + port, rc);
    }

    private void ensureParallelRequestsDontDegradePerformance(HttpServer server) {
        ThreadPoolConfig config = ThreadPoolConfig.defaultConfig().
                setPoolName("HttpRequestThreads").
                setCorePoolSize(10).
                setMaxPoolSize(500);

        NetworkListener listener = server.getListeners().iterator().next();
        GrizzlyExecutorService threadPool = (GrizzlyExecutorService) listener.getTransport().getWorkerThreadPool();
        threadPool.reconfigure(config);
    }

    public boolean storeSslCert(File certDirectory, InputStream key, InputStream trust) {
        File destKey = new File(certDirectory, "keystore.jks");
        File destTrust = new File(certDirectory, "truststore.jks");
        try {
            copyStreamToFile(destKey, key);
            copyStreamToFile(destTrust, trust);
        } catch (IOException e) {
            logger.warn("Unable to write cert file", e);
            return false;
        }

        // check certs correctness
        SSLContextConfigurator sslContext = new SSLContextConfigurator();
        File keyStoreFile = new File(certDirectory, "keystore.jks");
        sslContext.setKeyStoreFile(keyStoreFile.getAbsolutePath()); // contains server key
        sslContext.setKeyStorePass("hd84w19!");
        File trustStoreFile = new File(certDirectory, "truststore.jks");
        sslContext.setTrustStoreFile(trustStoreFile.getAbsolutePath());
        return sslContext.validateConfiguration(true);
    }

    private void copyStreamToFile(File file, InputStream stream) throws IOException {
        FileOutputStream fop = new FileOutputStream(file);
        byte[] buf = new byte[65536];
        for (; ; ) {
            int l = stream.read(buf);
            if (l < 0)
                break;
            fop.write(buf, 0, l);
        }
        fop.close();
    }
}
