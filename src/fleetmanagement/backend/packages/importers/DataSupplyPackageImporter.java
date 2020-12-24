package fleetmanagement.backend.packages.importers;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageSize;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.repositories.exception.PackageImportException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static fleetmanagement.backend.packages.PackageType.DataSupply;

public class DataSupplyPackageImporter implements PackageImporter {
    private static final String VERSION_LINE_PREFIX = "Versionsnummer=";
    private static final String VTS_FILE = "IBIS_SYS" + File.separator + "IBIS.VTS";
    private static final String FTS_FILE = "IBIS_SYS" + File.separator + "IBIS.FTS";
    private static final String START_OF_PERIOD = "BeginnPeriode=";
    private static final String END_OF_PERIOD = "EndePeriode=";
    private static SimpleDateFormat standardDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private static SimpleDateFormat ODBCDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String DB_FILE = "travis.db";

    private static final Logger logger = Logger.getLogger(DataSupplyPackageImporter.class);

    private final SimpleSlotFinder routeFinder = new SimpleSlotFinder("ibis_old", "container_old");
    private final DirectoryPatternSlotFinder ibisFinder = new DirectoryPatternSlotFinder("ibis");
    private final DirectoryPatternSlotFinder travisFinder = new DirectoryPatternSlotFinder("datasupply");

    @Override
    public boolean canImportPackage(String filename, File importDirectory) {
        int numOfSlots = routeFinder.findSlots(importDirectory).size();
        if (numOfSlots > 0) {
            logger.info("Found route version of DV package.");
            return true;
        }

        numOfSlots = ibisFinder.findSlots(importDirectory).size();
        if (numOfSlots > 0) {
            logger.info("Found ibis version of DV package.");
            return true;
        }

        numOfSlots = travisFinder.findSlots(importDirectory).size();
        if (numOfSlots > 0) {
            logger.info("Found travis version of DV package.");
            return true;
        }
        return false;
    }

    @Override
    public Package importPackage(String filename, File importDirectory) throws IOException {
        List<Slot> travisSlots = travisFinder.findSlots(importDirectory);
        if (travisSlots.isEmpty()) {
            return importOldPackage(importDirectory);
        } else {
            return importTravisPackage(importDirectory);
        }
    }

    private Package importTravisPackage(File importDirectory) throws IOException {

        Slot slot = defineTravisSlot(importDirectory);
        File database = new File(importDirectory, slot.directory + File.separator + DB_FILE);

        Connection conn = null;
        try {
            String url = "jdbc:sqlite:" + database.getCanonicalPath();
            conn = DriverManager.getConnection(url);

            logger.info("Connection to SQLite has been established.");

            String sql = "SELECT start, end, version FROM TimeTable";
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                if (rs.next()) {
                    String startOfPeriod = standardDateFormat.format(ODBCDateFormat.parse(rs.getString("start")));
                    String endOfPeriod = standardDateFormat.format(ODBCDateFormat.parse(rs.getString("end")));
                    String version = rs.getString("version");

                    logger.info(startOfPeriod + "\t" +
                            endOfPeriod + "\t" +
                            version);
                    return new Package(UUID.randomUUID(), DataSupply, version, importDirectory,
                            new PackageSize(importDirectory), slot.slotNumber, startOfPeriod, endOfPeriod);
                } else {
                    logger.warn("No data in TimeTable, can't get validity information!");
                    return null;
                }
            } catch (SQLException e) {
                logger.warn("Can't query Travis database!", e);
                return null;
            } catch (ParseException e) {
                logger.warn("Can't parse validity period in Travis database!", e);
                return null;
            }
        } catch (SQLException e) {
            logger.warn("Can't open Travis database!", e);
            return null;
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                logger.warn("Can't close Travis database!", ex);
            }
        }
    }

    private Package importOldPackage(File importDirectory) throws IOException {
        removeCopyStickFiles(importDirectory);
        Slot slot = defineIbisSlot(importDirectory);
        String version = parseDataSupplyVersion(importDirectory, slot);
        String startOfPeriod = null;
        String endOfPeriod = null;
        try {
            File metadataFile = new File(importDirectory + File.separator + slot.directory + File.separator + VTS_FILE);
            if (metadataFile.exists() && !metadataFile.isDirectory()) {
                startOfPeriod = parseDataSupplyParam(importDirectory, slot.directory + File.separator + VTS_FILE, START_OF_PERIOD);
                endOfPeriod = parseDataSupplyParam(importDirectory, slot.directory + File.separator + VTS_FILE, END_OF_PERIOD);
            } else {
                metadataFile = new File(importDirectory + File.separator + slot.directory + File.separator + FTS_FILE);
                if (metadataFile.exists() && !metadataFile.isDirectory()) {
                    startOfPeriod = parseDataSupplyParam(importDirectory, slot.directory + File.separator + FTS_FILE, START_OF_PERIOD);
                    endOfPeriod = parseDataSupplyParam(importDirectory, slot.directory + File.separator + FTS_FILE, END_OF_PERIOD);
                } else {
                    logger.warn("Can't read validity period of DV from " + importDirectory.toString());
                }
            }
        } catch (PackageImportException e) {
            logger.warn("Can't read validity period of DV from " + importDirectory.toString());
            logger.debug(e.toString(), e);
        }
        return new Package(UUID.randomUUID(), DataSupply, version, importDirectory,
                new PackageSize(importDirectory), slot.slotNumber, startOfPeriod, endOfPeriod);
    }

    @Override
    public PackageType getPackageType() {
        return DataSupply;
    }

    private String parseDataSupplyParam(File importDirectory, String filename, String key) {
        File znt = PathUtil.findCaseInsensitive(importDirectory, filename);
        List<String> lines = null;
        try {
            lines = FileUtils.readLines(znt);
        } catch (IOException e) {
            throw new PackageImportException(DataSupply, "Unable to extract \"" + key + "\" from \"" + filename + "\"! Reason: " + e.toString());
        }
        String valueLine = findLineStartingWith(lines, key);

        if (valueLine == null)
            throw new PackageImportException(DataSupply, "Unable to extract \"" + key + "\" from \"" + filename + "\"! Reason: key not found.");

        return valueLine.replace(key, "");

    }

    private void removeCopyStickFiles(File importDirectory) throws IOException {
        for (String f : Arrays.asList("copystick.ini", "copycard.ini")) {
            File ini = PathUtil.findCaseInsensitive(importDirectory, f);
            ini.delete();
        }
    }

    private Slot defineIbisSlot(File importDirectory) throws IOException {
        List<Slot> slots = ibisFinder.findSlots(importDirectory);
        slots.addAll(routeFinder.findSlots(importDirectory));
        removeEmptySlots(slots, importDirectory);
        if (slots.size() > 1)
            throw new PackageImportException(DataSupply, "ZIP file contains multiple data supplies for multiple slots.");

        return slots.get(0);
    }

    private Slot defineTravisSlot(File importDirectory) throws IOException {
        List<Slot> slots = travisFinder.findSlots(importDirectory);
        removeEmptySlots(slots, importDirectory);
        if (slots.size() > 1)
            throw new PackageImportException(DataSupply, "ZIP file contains multiple data supplies for multiple slots.");

        return slots.get(0);
    }

    private void removeEmptySlots(List<Slot> slots, File importDirectory) throws IOException {
        for (Slot s : new ArrayList<>(slots)) {
            File f = PathUtil.findCaseInsensitive(importDirectory, s.directory);
            if (f.list().length == 0) {
                Files.delete(f.toPath());
                slots.remove(s);
            }
        }
    }

    private String parseDataSupplyVersion(File importDirectory, Slot slot) throws IOException {
        File znt = PathUtil.findCaseInsensitive(importDirectory, slot.directory + "/IBIS_SYS/IBIS.ZNT");
        List<String> lines = FileUtils.readLines(znt);
        String versionLine = findLineStartingWith(lines, VERSION_LINE_PREFIX);

        if (versionLine == null)
            throw new PackageImportException(DataSupply, "Unable to extract data supply version from IBIS.znt");

        return versionLine.replace(VERSION_LINE_PREFIX, "");
    }

    private String findLineStartingWith(List<String> lines, String prefix) {
        return lines.stream()
                .map(x -> x.trim())
                .filter(x -> x.startsWith(prefix))
                .findFirst().orElse(null);
    }

    public static class DirectoryPatternSlotFinder {
        private final String regex;

        public DirectoryPatternSlotFinder(String slotName) {
            regex = "^" + slotName + "(\\d|$)";
        }

        public List<Slot> findSlots(File directory) {
            File[] files = directory.listFiles((FileFilter) new RegexFileFilter(regex, IOCase.INSENSITIVE));
            return Arrays.stream(files)
                    .filter(File::isDirectory)
                    .map(file -> {
                        String slotNumber = file.getName().replaceAll("[^0-9]", "");
                        return new Slot(slotNumber.isEmpty() ? 1 : Integer.valueOf(slotNumber), file.getName());
                    })
                    .collect(Collectors.toList());
        }
    }

    public static class SimpleSlotFinder {
        private final List<String> directoryNames;

        public SimpleSlotFinder(String... directoryNames) {
            this.directoryNames = Arrays.stream(directoryNames)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }

        public List<Slot> findSlots(File directory) {
            File[] files = directory.listFiles();
            return Arrays.stream(files)
                    .filter(file -> file.isDirectory() && directoryNames.contains(file.getName().toLowerCase()))
                    .map(file -> new Slot(0, file.getName()))
                    .collect(Collectors.toList());
        }
    }

    static class Slot {
        public final int slotNumber;
        public final String directory;

        private Slot(int number, String directory) {
            this.slotNumber = number;
            this.directory = directory;
        }
    }

}
