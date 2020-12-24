package fleetmanagement.backend;

import fleetmanagement.backend.groups.GroupsWatcher;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.repositories.disk.OnDiskVehicleRepository;
import fleetmanagement.backend.repositories.disk.xml.ProtocolXmlFile;
import fleetmanagement.backend.vehiclecommunication.FilteredUploadResource;
import fleetmanagement.backend.vehiclecommunication.LoginResource;
import fleetmanagement.backend.vehiclecommunication.TasksResource;
import fleetmanagement.backend.vehiclecommunication.UploadResource;
import fleetmanagement.backend.vehicles.OfflineMonitor;
import fleetmanagement.backend.webserver.BackendExceptionMapper;
import fleetmanagement.config.FimConfig;
import fleetmanagement.usecases.StorageManager;
import fleetmanagement.webserver.RequestLoggingFilter;
import fleetmanagement.webserver.Webserver;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;

@Component
public class Backend {
    private static final Logger logger = Logger.getLogger(Backend.class);

    @Autowired
    private FimConfig config;
    @Autowired
    private OnDiskVehicleRepository vehicleRepository;
    private Webserver[] webserver;
    @Autowired
    private GroupsWatcher watcher;
    @Autowired
    private NotificationService notificationService;
    private ProtocolXmlFile protocolXmlFile;
    @Autowired
    private StorageManager storageManager;
    @Autowired
    private OfflineMonitor offlineMonitor;

    @Autowired
    private TasksResource tasksResource;
    @Autowired
    private UploadResource uploadResource;
    @Autowired
    private FilteredUploadResource filteredUploadResource;

    public void start() throws Exception {
        // create webservers
        File backendDirectory = config.getBackendDirectory();
        if (!backendDirectory.exists()) {
            if (!backendDirectory.mkdir()) {
                logger.error("Unable to create backend data dir at " + backendDirectory.getAbsolutePath());
            }
        }

        protocolXmlFile = new ProtocolXmlFile(backendDirectory);

        webserver = new Webserver[3];

        for (int i = 0; i < 3; i++) {
            File certDirectory = null;
            if (i > 0) {
                certDirectory = new File(backendDirectory, "cert" + i);
                if (!certDirectory.exists()) {
                    if (!certDirectory.mkdir()) {
                        logger.error("Unable to create backend cert data dir at " + certDirectory.getAbsolutePath());
                    }
                }
            }
            webserver[i] = new Webserver(config.backendPort[i], i > 0, certDirectory);
        }

        for (int i = 0; i < 3; i++) {
            webserver[i].addExceptionMapper(new BackendExceptionMapper(notificationService));
            webserver[i].addRequestFilter(new RequestLoggingFilter());
            webserver[i].addResponseFilter(new RequestLoggingFilter());

            webserver[i].addResource(new LoginResource(vehicleRepository, i));
            webserver[i].addResource(tasksResource);
            webserver[i].addResource(uploadResource);
            webserver[i].addResource(filteredUploadResource);
        }

        storageManager.start();
        offlineMonitor.start();

        watcher.start();

        for (int i = 0; i < 3; i++) {
            if (protocolXmlFile.protocolConfig[i].enabled)
                webserver[i].start();
        }
    }

    public HttpServer getHttp() {
        return webserver[0].getHttp();
    }

    public String getProtocolName(int i) {
        if (i == 0) {
            return "http";
        } else {
            return "https " + i;
        }
    }

    public boolean getProtocolState(int i) {
        return protocolXmlFile.protocolConfig[i].enabled;
    }

    public int getProtocolPort(int i) {
        return config.backendPort[i];
    }

    public void stop() {
        watcher.shutDownListener();
        storageManager.stop();
        offlineMonitor.stop();
        for (int i = 0; i < 3; i++) {
            webserver[i].stop();
        }
    }

    public void enableProtocol(int p) throws Exception {
        if (protocolXmlFile.protocolConfig[p].enabled)
            return;

        protocolXmlFile.protocolConfig[p].enabled = true;
        protocolXmlFile.save();

        webserver[p].start();
    }

    public void disableProtocol(int p) {
        if (!protocolXmlFile.protocolConfig[p].enabled)
            return;

        protocolXmlFile.protocolConfig[p].enabled = false;
        protocolXmlFile.save();

        webserver[p].stop();
    }

    public boolean storeCert(int p, InputStream key, InputStream trust) {
        if (p == 0)
            return false;

        if (protocolXmlFile.protocolConfig[p].enabled)
            return false;

        File certDirectory = new File(config.getBackendDirectory(), "cert" + p);

        return webserver[p].storeSslCert(certDirectory, key, trust);
    }

}
