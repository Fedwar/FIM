package fleetmanagement.backend.vehiclecommunication.upload.filter;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UploadFilterConditionTest {




    @Test
    public void matchesEnding() {
        matchesRegexTypes("commercial.log", "*.log");
    }

    @Test
    public void matchesBeginningAndEnding() {
        matchesRegexTypes("commercial.log", "com*.log");
    }

    @Test
    public void matchesBeginning() {
        matchesRegexTypes("commercial.log", "com*");
    }

    @Test
    public void matchesMiddle() {
        matchesRegexTypes("commercial.log", "*merc*");
    }

    @Test
    public void matchesMultiAsterisk() {
        matchesRegexTypes("commercial.log", "*rc*.lo*");
    }

    @Test
    public void notMatchesDot() {
        UploadFilterCondition tested = new UploadFilterCondition(ConditionType.FILE_NAME, "*.log");
        assertFalse(tested.matches("commerciallog"));
    }

    @Test
    public void notMatchesEnding() {
        notMatchesRegexTypes("readme.txt", "*.log");
    }

    @Test
    public void notMatchesBeginningAndEnding() {
        notMatchesRegexTypes("readme.txt", "com*.log");
    }

    @Test
    public void notMatchesBeginning() {
        notMatchesRegexTypes("readme.txt", "com*");
    }

    @Test
    public void notMatchesMiddle() {
        notMatchesRegexTypes("readme.txt", "*merc*");
    }

    @Test
    public void notMatchesMultiAsterisk() {
        notMatchesRegexTypes("readme.txt", "*rc*.lo*");
    }



    private void matchesRegexTypes(String value, String matchString) {
        for (ConditionType regexType : UploadFilterCondition.regexTypes) {
            UploadFilterCondition tested = new UploadFilterCondition(regexType, matchString);
            assertTrue(tested.matches(value));
        }
    }

    private void notMatchesRegexTypes(String value, String matchString) {
        for (ConditionType regexType : UploadFilterCondition.regexTypes) {
            UploadFilterCondition tested = new UploadFilterCondition(regexType, matchString);
            assertFalse(tested.matches(value));
        }
    }



}