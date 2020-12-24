package fleetmanagement.backend.repositories.disk;

import fleetmanagement.TempFileRule;
import fleetmanagement.backend.widgets.Widget;
import fleetmanagement.backend.widgets.WidgetType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class WidgetXmlRepositoryTest {

    private WidgetXmlRepository tested;

    @Rule
    public TempFileRule tempDir = new TempFileRule();


    @Before
    public void setup() throws Exception {
        tested = new WidgetXmlRepository(tempDir);
    }

    @Test
    public void findsWidgetByIndicatorId()  {
        addWidget("indicator1");
        Widget widget = addWidget("indicator2");
        addWidget("indicator3");

        Widget found = tested.findWidgetByIndicatorId(widget.indicatorId);
        assertNotNull(widget);
        assertEquals(widget.indicatorId, found.indicatorId);
    }

    @Test
    public void deletesWidgetByIndicatorId() throws Exception {
        Widget widget1 = addWidget("indicator1");
        Widget widget2 = addWidget("indicator2");
        Widget widget3 = addWidget("indicator3");

        assertEquals(3,tested.persistables.size());
        tested.deleteWidgetByIndicatorId(widget2.indicatorId);
        assertEquals(2,tested.persistables.size());
        assertTrue(tested.persistables.contains(widget1));
        assertTrue(tested.persistables.contains(widget3));
    }
    
    Widget addWidget(String indicatorId) {
        Widget widget = new Widget(indicatorId, 1, 10, WidgetType.BAR);
        tested.insert(widget);
        return widget;
    }
}