package fleetmanagement.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CommandsGenerator {

    private static final Pattern SEED_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");
    private static final String SEED_CONV = "6429UPGm8tJ3NSM6";
    public static final String INSCODE_JSON = "inscode.json";

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static Cipher initCipher(int cipherMode, String seed) {
        java.security.MessageDigest messageDigest;
        try {
            messageDigest = java.security.MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Can't build the password, MD5 is not available");
            return null;
        }

        String passwordSource = SEED_CONV + seed;
        byte[] keyBytes = messageDigest.digest(passwordSource.getBytes(UTF_8));

        byte[] ivBytes = "fawCvX=F6_eNZ^Vh".getBytes();
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            System.out.println("Can't encrypt the data, encryption engine is not available");
            e.printStackTrace();
            return null;
        }

        try {
            cipher.init(cipherMode, key, ivSpec);
        } catch (InvalidKeyException e) {
            System.out.println("Can't encrypt the data, key is invalid");
            return null;
        } catch (InvalidAlgorithmParameterException e) {
            System.out.println("Can't encrypt the data, parameters are invalid");
            e.printStackTrace();
            return null;
        }

        return cipher;
    }

    public static String encrypt(String seed, byte[] input) {
        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, seed);

        byte[] encrypted = new byte[cipher.getOutputSize(input.length)];

        int enc_len = 0;
        try {
            enc_len = cipher.update(input, 0, input.length, encrypted, 0);
        } catch (ShortBufferException e) {
            System.out.println("Can't encrypt the data, buffer is too short");
            return "";
        }

        try {
            enc_len += cipher.doFinal(encrypted, enc_len);
        } catch (IllegalBlockSizeException e) {
            System.out.println("Can't encrypt the data, illegal block size");
            return "";
        } catch (ShortBufferException e) {
            System.out.println("Can't encrypt the data, buffer is too short");
            return "";
        } catch (BadPaddingException e) {
            System.out.println("Can't encrypt the data, invalid padding");
            return "";
        }

        String encryptedString = javax.xml.bind.DatatypeConverter.printHexBinary(encrypted);

        return encryptedString;
    };

    public static String encrypt(String seed, String command) {
        byte[] bytes = command.getBytes();
        return encrypt(seed, bytes);
    }

    public static String decrypt(String seed, String command) throws ShortBufferException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = initCipher(Cipher.DECRYPT_MODE, seed);

        byte[] encrypted = hexStringToByteArray(command);

        int enc_len = encrypted.length;
        byte[] decrypted = new byte[cipher.getOutputSize(enc_len)];
        int dec_len = cipher.update(encrypted, 0, enc_len, decrypted, 0);
        dec_len += cipher.doFinal(decrypted, dec_len);
        return new String(decrypted, UTF_8);
    }

    public static void verifySeed(String seed) throws Exception {
        if (!SEED_PATTERN.matcher(seed).matches()) {
            throw new Exception("Seed is not a valid MD5 hash, 32 digit hexadecimal expected.");
        }
    }

    public static void verifyJson(byte[] input) throws Exception {
        String inputString = new String(input, UTF_8);
        try {
            JsonReader jsonReader = new JsonReader(new StringReader(inputString));
            jsonReader.setLenient(false);
            new Gson().fromJson(jsonReader, Command.class);
        } catch (Exception e) {
            throw new Exception("Not a valid json: " + e.getMessage(), e);
        }
    }

    private static boolean verify = false;
    private static String seed;
    private static String jsonFile;
    private static String encryptedFile = INSCODE_JSON;

    private static void parseArgs(String[] args) {
        checkParamCount(args, 2);
        if (args[0].equals("-q")) {
            checkParamCount(args, 3);
            seed = args[1];
            encryptedFile = args[2];
        } else if (args[0].equals("-v")) {
            checkParamCount(args, 3);
            seed = args[1];
            jsonFile = args[2];
        } else {
            seed = args[0];
            jsonFile = args[1];
        }
    }

    private static void checkParamCount(String[] args, int count) {
        if (args.length < count) {
            usageAndExit();
        }
    }

    private static void usageAndExit() {
        System.out.println("Usage:\n" +
                "  [-v] <seed> <filename>       : encrypt license (optionally with verify)\n" +
                "  -q <seed> <filename>         : query encrypted license");
        System.exit(1);
    }

    private static void doEncrypt() {
        System.out.println("seed = " + seed);

        byte[] input;
        try {
            input = Files.readAllBytes(new File(jsonFile).toPath());
        } catch (IOException e) {
            System.out.println("Can't read " + jsonFile);
            return;
        }

        if (verify) {
            try {
                verifySeed(seed);
                verifyJson(input);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
        }

        System.out.println("Copy the below data between \"---\" and send it to the customer.");
        System.out.println("---");
        String encryptedString = encrypt(seed, input);
        System.out.println(encryptedString);
        System.out.println("---");

        // Decrypting back to verify

        try (PrintWriter out = new PrintWriter(encryptedFile)) {
            out.print(encryptedString);
        } catch (FileNotFoundException e) {
            System.out.println("Can't save the licence into file: " + encryptedFile);
        }

        try {
            System.out.println(decrypt(seed, encryptedString));
        } catch (BadPaddingException | IllegalBlockSizeException | ShortBufferException e) {
            e.printStackTrace();
        }
    }

    private static void doQuery() {
        try {
            String encryptedString = new String(Files.readAllBytes(Paths.get(encryptedFile)), UTF_8);
            String decrypted = decrypt(seed, encryptedString);
            System.out.println("License contents:\n" + decrypted);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        parseArgs(args);
        if (jsonFile != null) {
            doEncrypt();
        } else {
            doQuery();
        }
    }
}
