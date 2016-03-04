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
		new ShutdownThread();
	}

	private static class ShutdownThread implements Runnable {

		private ShutdownThread() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				Qwazr.logger.warn(e.getMessage(), e);
			}
			Qwazr.qwazr.stopAll();
			System.exit(0);
		}
	}
}
