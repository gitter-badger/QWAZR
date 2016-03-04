package com.qwazr;

import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@RolesAllowed("welcome")
@Path("/")
@ServiceName("welcome")
public class WelcomeServiceImpl implements ServiceInterface {

	@GET
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	public WelcomeStatus welcome() {
		return new WelcomeStatus(Qwazr.qwazr.services);
	}

	@DELETE
	@Path("/shutdown")
	public void shutdown() {
		System.exit(0);
	}
}
