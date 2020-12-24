package fleetmanagement.config;

import fleetmanagement.AppConfig;
import fleetmanagement.TempFileRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;

import static org.junit.Assert.*;

@Ignore
public class FimConfigTest {

	@Rule
	public TempFileRule tempFolder = new TempFileRule();

/*
	@Test
	public void readsConfigFromDirectory() throws Exception {
		Properties properties = new Properties();
		properties.setProperty("FrontendPort", String.valueOf(12));
		properties.setProperty("BackendPort", String.valueOf(23));
		properties.setProperty("BackendHttps1Port", String.valueOf(34));
		properties.setProperty("BackendHttps2Port", String.valueOf(45));
		properties.setProperty("UseHttpsByDefault", String.valueOf(true));
		try (OutputStream output = new FileOutputStream(new File(tempFolder, AppConfig.CONFIG_FILE))) {
			properties.store(output, null);
		}

		FimConfig tested = new FimConfig(tempFolder);

		assertEquals(12, tested.frontendPort);
		assertEquals(23, tested.backendPort[0]);
		assertEquals(34, tested.backendPort[1]);
		assertEquals(45, tested.backendPort[2]);
		assertTrue(tested.httpsByDefault);
	}
	@Test
	public void readsConfigFromRootFolder() throws Exception {
		Properties properties = new Properties();
		properties.setProperty("FrontendPort", String.valueOf(12));
		properties.setProperty("BackendPort", String.valueOf(23));
		properties.setProperty("BackendHttps1Port", String.valueOf(34));
		properties.setProperty("BackendHttps2Port", String.valueOf(45));
		properties.setProperty("UseHttpsByDefault", String.valueOf(true));
		try (OutputStream output = new FileOutputStream(new File(tempFolder, FimConfig.CONFIG_FILE))) {
			properties.store(output, null);
		}

		FimConfig tested = new FimConfig(tempFolder);

		assertEquals(12, tested.frontendPort);
		assertEquals(23, tested.backendPort[0]);
		assertEquals(34, tested.backendPort[1]);
		assertEquals(45, tested.backendPort[2]);
		assertTrue(tested.httpsByDefault);
	}




	@Test
	public void returnsDefaultPropertiesForConfig() {
		FimConfig tested = new FimConfig(tempFolder);
		
		assertEquals(29668, tested.frontendPort);
		assertEquals("Import", tested.getGroupImport());
		assertEquals("Incoming", tested.getFilterIncoming());
		assertEquals(29667, tested.backendPort[0]);
		assertEquals(29669, tested.backendPort[1]);
		assertEquals(29670, tested.backendPort[2]);
		assertFalse(tested.httpsByDefault);
	}
*/
}
