package fleetmanagement.config;


import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import fleetmanagement.backend.packages.PackageType;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class LicenceImpl implements Licence {

    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Logger logger = Logger.getLogger(LicenceImpl.class);
    private static final Gson gson = new Gson();
    private String commandFileName;

    private LicenceInfo licenceInfo;
    private final String installationSeed;
    private File dataDirectory;

    public LicenceImpl(File dataDirectory) {
        this.dataDirectory = dataDirectory;

        try {
            commandFileName = dataDirectory.getCanonicalPath() + File.separator + "licence.txt";
        } catch (IOException e) {
            logger.warn("Can't build path to the licence file!");
            logger.debug(e.getMessage(), e);
        }

        installationSeed = generateSeed(NetworkHelper.getMacAddressDummy());

        if (this.commandFileName != null) {
            File licenceFile = new File(this.commandFileName);
            if (licenceFile.exists()) {
                byte[] encoded;
                try {
                    encoded = Files.readAllBytes(new File(commandFileName).toPath());
                    update(new String(encoded, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    logger.error("Can't read licence from file: " + commandFileName);
                    logger.debug(e.getMessage(), e);
                }
            } else {
                update();
            }
        }
    }

    private ArrayList<String> getSeeds() {
        ArrayList<String> seeds = new ArrayList<>();
        seeds.add(installationSeed);
        try {
            Set<String> allMacAddresses = NetworkHelper.getAllMacAddresses();
            seeds.add(generateSeed(NetworkHelper.getMacAddressDummy()));
            seeds.add(generateSeed(StringUtils.join(allMacAddresses, "")));
            for (String macAddress : allMacAddresses) {
                seeds.add(generateSeed(macAddress));
            }
            return seeds;
        } catch (SocketException e) {
            logger.error("Can't get network addresses.");
            logger.debug(e.toString(), e);
            return seeds;
        }
    }

    private String generateSeed(String macAddress) {
        if (macAddress == null)
            return "";

        String path;
        try {
            path = dataDirectory.getCanonicalPath();
        } catch (IOException e) {
            logger.error("Can't generate installation seed! Can't build canonical path to data directory.");
            logger.debug(e.toString(), e);
            return null;
        }

        java.security.MessageDigest messageDigest;
        try {
            messageDigest = java.security.MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Can't generate installation seed! MD5 is not available.");
            logger.debug(e.toString(), e);
            return null;
        }

        String SEED_SALT = "g9D&95j&KZCjJ5&v";
        String source = SEED_SALT + path + macAddress;
        byte[] digest = messageDigest.digest(source.getBytes(StandardCharsets.UTF_8));

        return javax.xml.bind.DatatypeConverter.printHexBinary(digest);
    }

    public String decode(String commandText) throws Exception {
        Exception decodeException = null;
        ArrayList<String> seeds = getSeeds();
        for (String seed : seeds) {
            try {
                String commandString = CommandsGenerator.decrypt(seed, commandText);
                return commandString;
            } catch (BadPaddingException | ShortBufferException | IllegalBlockSizeException e) {
                decodeException = e;
            }
        }
        throw decodeException;
    }

    @Override
    public String getInstallationSeed() {
        return installationSeed;
    }

    @Override
    public boolean isExpired() {
        return licenceInfo == null || licenceInfo.expired != null && licenceInfo.expired.isBefore(ZonedDateTime.now());
    }

    @Override
    public boolean isMapAvailable() {
        return licenceInfo != null && licenceInfo.mapAvailable;
    }

    @Override
    public boolean isDiagnosisInfoAvailable() {
        return licenceInfo != null && licenceInfo.diagnosisInfo;
    }

    @Override
    public boolean isOperationInfoAvailable() {
        return licenceInfo != null && licenceInfo.operationInfo;
    }

    @Override
    public boolean isVehicleGeoAvailable() {
        return licenceInfo != null && licenceInfo.vehicleGeoAvailable;
    }

    @Override
    public boolean isAutoPackageSyncAvailable() {
        return licenceInfo != null && licenceInfo.autoPackageSync;
    }

    @Override
    public boolean isUploadAvailable() {
        return licenceInfo != null && licenceInfo.upload;
    }

    @Override
    public boolean isNotificationsAvailable() {
        return licenceInfo != null && licenceInfo.notifications;
    }

    @Override
    public boolean isHttpsAvailable() {
        return licenceInfo != null && licenceInfo.https;
    }

    @Override
    public boolean isReportsAvailable() {
        return licenceInfo != null && licenceInfo.reports;
    }

    @Override
    public boolean isPackageTypeAvailable(PackageType packageType) {
        return licenceInfo != null && licenceInfo.availablePackageTypes.contains(packageType);
    }


    @Override
    public void update() {
        HashSet<PackageType> packageTypes = new HashSet<>();
        packageTypes.add(PackageType.DataSupply);
        licenceInfo = new LicenceInfo(
                null,
                5,
                false,
                false,
                packageTypes,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Arrays.asList("en", "de"),
                false);
    }

    @Override
    public boolean update(String commandText) {
        String commandString = null;

        try {
            commandString = decode(commandText);
        } catch (Exception e) {
            logger.error("Can't decrypt the command!");
            logger.debug(e.getMessage(), e);
            return false;
        }

        try {
            JsonReader jsonReader = new JsonReader(new StringReader(commandString));
            jsonReader.setLenient(true);
            Command command = gson.fromJson(jsonReader, Command.class);
            ZonedDateTime date;
            if (command.date == null) {
                date = null;
            } else {
                LocalDate localDate = LocalDate.parse(command.date, dateFormat);
                date = localDate.atStartOfDay(ZoneId.systemDefault());
            }
            Set<PackageType> packageTypes;
            if (command.packages == null) {
                packageTypes = (licenceInfo == null ? new HashSet<>() : licenceInfo.availablePackageTypes);
            } else {
                packageTypes = Arrays.stream(command.packages).map(PackageType::getByResourceKey)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
            List<String> languages;
            if (command.languages == null) {
                languages = licenceInfo == null ? Arrays.asList("en", "de") : licenceInfo.languages;
            } else {
                languages = new LinkedList<>(Arrays.asList(command.languages));
            }
            licenceInfo = new LicenceInfo(date, command.vehicles,
                    command.geo, command.vehicleGeo, packageTypes, command.diagnosis,
                    command.operation, command.autoPackageSync, command.upload, command.notifications,
                    command.https, command.reports, languages, command.vehicleIp);
        } catch (Exception e) {
            logger.error("Decrypted licence command has invalid format!");
            logger.debug(e.getMessage(), e);
            return false;
        }
        return true;
    }

    public void saveLicenceToFile(String licence) {
        try (PrintWriter out = new PrintWriter(commandFileName)) {
            out.print(licence);
        } catch (FileNotFoundException e) {
            logger.error("Can't save the licence into file: " + commandFileName);
            logger.debug(e.getMessage(), e);
        }
    }

    @Override
    public boolean isLoaded() {
        return licenceInfo != null;
    }

    @Override
    public int getMaximumVehicleCount() {
        return licenceInfo == null ? 0 : licenceInfo.vehicleCount;
    }

    @Override
    public String getExpirationDate() {
        if (licenceInfo == null || licenceInfo.expired == null) {
            return null;
        }
        return licenceInfo.expired.format(dateFormat);
    }

    @Override
    public Set<PackageType> getPackageTypes() {
        if (licenceInfo == null || licenceInfo.availablePackageTypes == null)
            return Collections.EMPTY_SET;
        return licenceInfo.availablePackageTypes;
    }

    @Override
    public List<String> getLanguages() {
        if (licenceInfo == null || licenceInfo.languages == null)
            return Collections.EMPTY_LIST;
        return licenceInfo.languages;
    }

    @Override
    public boolean isVehicleIpAvailable() {
        return licenceInfo != null && licenceInfo.vehicleIp;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LicenceImpl licence = (LicenceImpl) o;
        return Objects.equals(licenceInfo, licence.licenceInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licenceInfo);
    }
}
