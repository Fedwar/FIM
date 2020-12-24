package fleetmanagement.frontend.controllers;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.webserver.ModelAndView;
import fleetmanagement.usecases.ShowDashboardOverview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Path("/")
@Component
public class Dashboard extends FrontendController {
	
	private final ShowDashboardOverview showDashboardOverview;

	@Autowired
	public Dashboard(UserSession session, ShowDashboardOverview showDashboardOverview) {
		super(session);
		this.showDashboardOverview = showDashboardOverview;
	}

	@GET
	public Response getIndex() {
		return Redirect.to("/dashboard");
	}
	
	@GET
	@Path("/dashboard")
	public ModelAndView<fleetmanagement.frontend.model.Dashboard> getDashboardUI() {
		fleetmanagement.frontend.model.Dashboard vm = showDashboardOverview.createDashboard(session);
		return new ModelAndView<>("dashboard.html", vm);
	}

	@GET
	@Path("ajax/dashboard")
	public ModelAndView<fleetmanagement.frontend.model.Dashboard> getDashboardData() {
		fleetmanagement.frontend.model.Dashboard vm = showDashboardOverview.createDashboard(session);
		return new ModelAndView<>("dashboard-template.html", vm);
	}


}
