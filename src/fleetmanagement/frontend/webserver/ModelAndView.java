package fleetmanagement.frontend.webserver;

public class ModelAndView<T> {

	public final String page;
	public final T viewmodel;

	public ModelAndView(String page, T viewmodel) {
		this.page = page;
		this.viewmodel = viewmodel;
	}
}
