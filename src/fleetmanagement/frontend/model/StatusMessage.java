package fleetmanagement.frontend.model;


public class StatusMessage {

	public String text;
	public String cssClass;
	
	public StatusMessage(String text, String cssClass) {
		this.text = text;
		this.cssClass = cssClass;
	}
	
	public static class SuccessMessage extends StatusMessage {
		public SuccessMessage(String text) {
			super(text, "alert-success");
		}
	}
	
	public static class ErrorMessage extends StatusMessage {
		public ErrorMessage(String text) {
			super(text, "alert-danger");
		}
	}
}
