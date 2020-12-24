package fleetmanagement.frontend.controllers;

import static org.junit.Assert.*;

import org.htmlcleaner.*;

public class HtmlAssert {
	
	public static void assertIsValidHtml(String html) throws Exception {
		TagNode root = new HtmlCleaner().clean(html);
		assertEquals("html", root.getName());		
		assertEquals(1, root.evaluateXPath("head").length);
		assertEquals(1, root.evaluateXPath("body").length);
	}
	
}
