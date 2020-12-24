package fleetmanagement.frontend.languages;

import fleetmanagement.config.Licence;
import fleetmanagement.test.LicenceStub;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LanguagesTest {
    private Licence licence = new LicenceStub();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void getsCorrectLanguagesFromConfig() throws IOException {
        createProperties();
        Languages languages = new Languages(temporaryFolder.getRoot(), licence);
        List<String> expected = Collections.singletonList("ru");

        List<String> actual = languages.getLanguages();

        expected.stream().filter(lang -> !actual.contains(lang))
                .forEach(lang -> fail("Element " + lang + " was not found"));
        assertEquals(expected.size(), actual.size());
    }

    @Test
    public void getsCorrectLanguagesWhenConfigIsNotExist() {
        Languages languages = new Languages(temporaryFolder.getRoot(), licence);
        List<String> expected = Arrays.asList("de", "es", "fr", "pl", "cs", "ru");

        List<String> actual = languages.getLanguages();

        expected.stream().filter(lang -> !actual.contains(lang))
                .forEach(lang -> fail("Element " + lang + " was not found"));
        assertEquals(expected.size(), actual.size());
    }

    private void createProperties() throws IOException {
        String fileName = "languages.properties";
        temporaryFolder.newFile(fileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(temporaryFolder.getRoot(), fileName)));
        writer.write("en=1");
        writer.newLine();
        writer.write("fr=0");
        writer.newLine();
        writer.write("ru=1");
        writer.close();
    }
}