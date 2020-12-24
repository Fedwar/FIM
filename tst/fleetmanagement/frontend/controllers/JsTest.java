package fleetmanagement.frontend.controllers;

import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.api.NotFoundException;

import fleetmanagement.test.SessionStub;

public class JsTest {
	
	private Js tested;
	
	@Before
	public void setup() {
		tested = new Js(new SessionStub());
	}
	
	@Test(expected=NotFoundException.class)
	public void returns404WhenRythmFileIsUnknown() {
		tested.getJsFile("unknown-file.rythm.js");
	}
	
	@Test(expected=NotFoundException.class)
	public void returns404WhenRegularFileIsUnknown() {
		tested.getJsFile("unknown-file.min.js");
	}
}
