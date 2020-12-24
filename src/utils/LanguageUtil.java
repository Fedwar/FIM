package utils;

import gsp.util.DoNotObfuscate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class LanguageUtil {

    private static final Logger logger = Logger.getLogger(LanguageUtil.class);
    private static final String XLSX_FILE = "C:/Temp/NewData.xlsx";
    private static XSSFWorkbook excelBook;
    private static XSSFCellStyle style;
    private static XSSFSheet sheet;
    private static int lastRowNum;


    private static void setExcelBook() {
        try {
            excelBook = new XSSFWorkbook(new FileInputStream(XLSX_FILE));
        } catch (IOException e) {
            logger.error("Can not create XSSFWorkbook object from " + XLSX_FILE);
        }
    }

    private static void setStyle() {
        style = excelBook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    public static void main(String[] args) {
        setExcelBook();
        setStyle();
        sheet = excelBook.getSheetAt(0);
        lastRowNum = sheet.getLastRowNum();

        //addEmptyValuesToExcel();
        //addNewExcelValuesToMessagesFiles();
        //updateMessagesFilesFromExcel();
        addNotAllLanguagesTranslatedValuesToExcel();
    }

    private static void addEmptyValuesToExcel() {
        HashMap<String, Message> excelMap = loadMapFromExcel(0, MESSAGES_FILE.DE.columnIndex);
        Properties properties = getProperties(MESSAGES_FILE.DE.filePath);
        List<Message> collection = getDifCollection(excelMap, properties);
        fillSheet(collection);
        writeToExcel(excelBook, "C:\\temp\\Kopie von FIM-54_GUI_Ressourcen_alleSprachen_korr_RU-AT_PL-MK_ES-JRB.xlsx");
    }

    private static void addNewExcelValuesToMessagesFiles() {
        for (MESSAGES_FILE messagesFile : MESSAGES_FILE.values()) {
            HashMap<String, Message> excelMap = loadMapFromExcel(1, messagesFile.columnIndex);
            Properties properties = getProperties(messagesFile.filePath);
            LinkedHashMap<String, String> newProperties = getNewProperties(excelMap, properties);
            appendToPropertiesFile(newProperties, messagesFile.filePath);
        }
    }

    private static void updateMessagesFilesFromExcel() {
        for (MESSAGES_FILE messagesFile : MESSAGES_FILE.values()) {
            HashMap<String, Message> excelMap = loadMapFromExcel(1, messagesFile.columnIndex);
            Properties properties = getProperties(messagesFile.filePath);
            Properties updatedProperties = getUpdatedProperties(excelMap, properties);
            LinkedHashMap<String, String> orderedUpdatedProperties
                    = getSortedUpdatedProperties(updatedProperties, messagesFile.filePath);
            clearPropertiesFile(messagesFile.filePath);
            appendToPropertiesFile(orderedUpdatedProperties, messagesFile.filePath);
        }
    }

    private static void addNotAllLanguagesTranslatedValuesToExcel() {
        List<Properties> allProperties = new LinkedList<>();
        for (MESSAGES_FILE messagesFile : MESSAGES_FILE.values())
            allProperties.add(getProperties(messagesFile.filePath));

        Set<String> notAllLanguagesTranslatedValues = new HashSet<>();
        for (Properties properties : allProperties){
            for (String key : properties.stringPropertyNames()) {
                for (Properties props : allProperties) {
                    if (!props.containsKey(key))
                        notAllLanguagesTranslatedValues.add(key);
                    else
                        if (props.getProperty(key) == null | props.getProperty(key).equals(""))
                            notAllLanguagesTranslatedValues.add(key);
                }
            }
        }

        XSSFWorkbook excelBook = new XSSFWorkbook();
        XSSFSheet sheet = excelBook.createSheet();
        int lastRowNum = 0;

        XSSFRow headerRow = sheet.createRow(lastRowNum);
        for (MESSAGES_FILE messagesFile : MESSAGES_FILE.values()) {
            XSSFCell cell = headerRow.createCell(messagesFile.columnIndex);
            cell.setCellValue(messagesFile.toString());
        }
        lastRowNum++;
        for (String value : notAllLanguagesTranslatedValues) {
            XSSFRow row = sheet.createRow(lastRowNum);
            for (MESSAGES_FILE messagesFile : MESSAGES_FILE.values()) {
                XSSFCell cell = row.createCell(messagesFile.columnIndex);
                cell.setCellValue(value + "=" + allProperties.get(messagesFile.columnIndex).getProperty(value));
            }
            lastRowNum++;
        }

        writeToExcel(excelBook, "C:/Temp/NotTranslated.xlsx");
    }

    private static HashMap<String, Message> loadMapFromExcel(int startRowIndex, int columnIndex) {
        HashMap<String, Message> map = new HashMap<>();
        for (int i = startRowIndex; i <= lastRowNum; i++) {
            XSSFRow row = sheet.getRow(i);
            if (row.getCell(columnIndex) == null)
                continue;
            String cellValue = row.getCell(columnIndex).getStringCellValue();
            if (cellValue != null && !cellValue.isEmpty()) {
                String[] split = cellValue.split("=");
                map.put(
                        split[0].replace(" ", ""),
                        new Message(split[0].replace(" ", ""), split.length == 1 ? null : split[1], i)
                );
            }
        }
        return map;
    }

    private static Properties getProperties(String propertiesFileName) {
        Properties properties = new Properties();
        String propStr;
        try {
            propStr = FileUtils.readFileToString(new File(propertiesFileName), Charset.forName("utf-8"));
        } catch (IOException e) {
            logger.error("Can not read " + propertiesFileName + " to string");
            return properties;
        }
        propStr = propStr.replace("\\\r\n", "\\\\\\n");
        try {
            properties.load(IOUtils.toInputStream(propStr));
        } catch (IOException e) {
            logger.error("Can not load properties from string");
        }
        return properties;
    }

    private static List<Message> getDifCollection(HashMap<String, Message> excelMap, Properties properties) {
        return properties.entrySet().stream()
                .map(e -> new Message((String) e.getKey(), (String) e.getValue(), -1))
                .peek(e -> {
                    Message message = excelMap.get(e.key);
                    if (message != null) {
                        e.oldValue = message.value;
                        e.strIndex = message.strIndex;
                    }
                })
                .filter(e -> !e.value.equals(e.oldValue))
                .sorted()
                .collect(Collectors.toList());
    }

    private static LinkedHashMap<String, String> getNewProperties(HashMap<String, Message> excelMap, Properties properties) {
        LinkedHashMap<String, String> newProperties = new LinkedHashMap<>();
        for (Message message : excelMap.values()) {
            if (!properties.containsKey(message.key.replaceAll(" ", "")))
                newProperties.put(message.key.replaceAll(" ", ""), convertToUnicode(message.value));
        }
        return newProperties;
    }

    private static Properties getUpdatedProperties(HashMap<String, Message> excelMap, Properties properties) {
        Properties updatedProperties = new Properties();
        //updatedProperties.putAll(properties);
        for (Object o : properties.keySet()) {
            String key = (String) o;
            updatedProperties.put(key, convertToUnicode(properties.getProperty(key)));
        }
        for (Message message : excelMap.values())
            updatedProperties.put(message.key.replaceAll(" ", ""), convertToUnicode(message.value));
        return updatedProperties;
    }

    private static LinkedHashMap<String, String> getSortedUpdatedProperties(Properties updatedProperties, String filePath) {
        LinkedHashMap<String, String> sortedUpdatedProperties = new LinkedHashMap<>();

        List<String> lines = new LinkedList<>();
        try {
            lines = Files.readAllLines(Paths.get(filePath));
        } catch (IOException e) {
            logger.error("Can not read " + filePath);
        }

        for (String line : lines) {
            if (!line.contains("="))
                continue;
            String key = line.split("=")[0].replace(" ", "");
            sortedUpdatedProperties.put(key, updatedProperties.getProperty(key));
            updatedProperties.remove(key);
        }

        updatedProperties.forEach((key, value) -> sortedUpdatedProperties.put(key.toString(), value.toString()));

        return sortedUpdatedProperties;
    }

    private static void fillSheet(List<Message> collect) {
        for (Message message : collect) {
            XSSFCell cell;
            if (message.strIndex >= 0) {
                cell = sheet.getRow(message.strIndex).getCell(0);
            } else {
                cell = sheet.createRow(lastRowNum++).createCell(0);
            }
            cell.setCellValue(message.key + "=" + message.value);
            cell.setCellStyle(style);
        }
    }

    private static void writeToExcel(XSSFWorkbook excelBook, String path) {
        try {
            FileOutputStream output_file = new FileOutputStream(new File(path));
            excelBook.write(output_file); //write changes
            output_file.close();  //close the stream
        } catch (IOException e) {
            logger.error("Error while writing to excel");
        }
    }

    private static void clearPropertiesFile(String filePath) {
        try {
            new PrintWriter(filePath).close();
        } catch (FileNotFoundException e) {
            logger.error("Can not find " + filePath + " to clear it's content");
        }
    }

    private static void appendToPropertiesFile(LinkedHashMap<String, String> map, String filePath) {
        try {
            Files.write(Paths.get(filePath), "\r\n".getBytes(), StandardOpenOption.APPEND);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                Files.write(
                        Paths.get(filePath),
                        (entry.getKey() + "=" + entry.getValue() + "\r\n").getBytes(),
                        StandardOpenOption.APPEND
                );
            }
        } catch (IOException e) {
            logger.error("Can not write in " + filePath);
        }
    }

    private static String convertToUnicode(String input) {
        StringBuilder b = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (c >= 128)
                b.append("\\u").append(String.format("%04X", (int) c));
            else
                b.append(c);
        }

        return b.toString();
    }

    private static String convertFromUnicode(String str) {
        return StringEscapeUtils.unescapeJava(str);
    }

    private static class Message implements Comparable {
        String key;
        String oldValue;
        String value;
        int strIndex;

        Message(String key, String value, int strIndex) {
            this.key = key;
            this.value = value;
            this.strIndex = strIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Message message = (Message) o;
            return Objects.equals(key, message.key) &&
                    Objects.equals(value, message.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }

        @Override
        public int compareTo(Object o) {
            return new CompareToBuilder()
                    .append(this.key, ((Message) o).key)
                    .toComparison();
        }
    }

    @DoNotObfuscate
    public enum MESSAGES_FILE {
        DE(0, "resources/messages_de.properties"),
        CZ(1, "resources/messages_cs.properties"),
        ES(2, "resources/messages_es.properties"),
        FR(3, "resources/messages_fr.properties"),
        PL(4, "resources/messages_pl.properties"),
        RU(5, "resources/messages_ru.properties"),
        EN(6, "resources/messages_en.properties");

        private int columnIndex;
        private String filePath;

        MESSAGES_FILE(int columnIndex, String filePath) {
            this.filePath = filePath;
            this.columnIndex = columnIndex;
        }
    }
}
