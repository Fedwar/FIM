package fleetmanagement.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandsGeneratorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void generatesEncryptedFile() throws IOException {
        File commandFile = tempFolder.newFile("command.json");
        File output = new File(CommandsGenerator.INSCODE_JSON);
        output.delete();

        assertFalse(output.exists());

        CommandsGenerator.main(new String[]{"seed", commandFile.getAbsolutePath()});

        assertTrue(output.exists());
        assertTrue(output.length() > 0);
    }

    @Test
    public void encrypt() throws BadPaddingException, ShortBufferException, IllegalBlockSizeException {
        final String seed = "seed";
        final String command = "command";
        String encrypted = CommandsGenerator.encrypt(seed, command);

        assertFalse(encrypted.isEmpty());

        String decrypted = CommandsGenerator.decrypt(seed, encrypted);
        //todo decrypted comes with trailing zero bytes
        assertEquals(command, decrypted.trim());
    }

    @Test
    public void verifyJson_valid() throws Exception {
        CommandsGenerator.verifyJson("{}".getBytes());
    }

    @Test(expected = Exception.class)
    public void verifyJson_invalid() throws Exception {
        CommandsGenerator.verifyJson("{,}".getBytes());
    }

    @Test
    public void verifySeed_valid() throws Exception {
        CommandsGenerator.verifySeed("4462031CFECCC1ED6DD3735EF8E9E285");
        CommandsGenerator.verifySeed("00000000000000000000000000000000");
        CommandsGenerator.verifySeed("abcdefffffffffffffffffffffffffff");
    }

    @Test(expected = Exception.class)
    public void verifySeed_longer() throws Exception {
        CommandsGenerator.verifySeed("4462031CFECCC1ED6DD3735EF8E9E2850");
    }

    @Test(expected = Exception.class)
    public void verifySeed_wrongCharacter() throws Exception {
        CommandsGenerator.verifySeed("4462031CFECCC1ED6DD3735EF8E9E28x");
    }

    @Test(expected = Exception.class)
    public void verifySeed_shorter() throws Exception {
        CommandsGenerator.verifySeed("4462031CFECCC1ED6DD3735EF8E9Ex");
    }
}