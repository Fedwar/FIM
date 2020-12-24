package fleetmanagement.backend.tasks;

import gsp.util.DoNotObfuscate;

@DoNotObfuscate
public class TaskJson {
	public final String type;
	public final String id;
	public final String url;
	
	public TaskJson(String type, String id, String url) {
		this.type = type;
		this.id = id;
		this.url = url;
	}
}
