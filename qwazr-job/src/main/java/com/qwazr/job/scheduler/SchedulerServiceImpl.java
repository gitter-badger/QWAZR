/**   
 * License Agreement for QWAZR
 *
 * Copyright (C) 2014-2015 OpenSearchServer Inc.
 * 
 * http://www.qwazr.com
 * 
 * This file is part of QWAZR.
 *
 * QWAZR is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * QWAZR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QWAZR. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.qwazr.job.scheduler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.TreeMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.utils.server.ServerException;

public class SchedulerServiceImpl implements SchedulerServiceInterface {

	private final static Logger logger = LoggerFactory
			.getLogger(SchedulerServiceImpl.class);

	@Override
	public TreeMap<String, String> list() {
		return SchedulerManager.INSTANCE.getSchedulers();
	}

	@Override
	public SchedulerStatus get(String scheduler_name, ActionEnum action) {
		try {
			SchedulerDefinition scheduler = SchedulerManager.INSTANCE
					.getScheduler(scheduler_name);
			SchedulerStatus schedulerStatus = new SchedulerStatus(scheduler);
			if (action == null)
				return schedulerStatus;
			Boolean enabled = null;
			switch (action) {
			case enable:
				enabled = true;
				break;
			case disable:
				enabled = false;
				break;
			case run:
				schedulerStatus.script_status = SchedulerManager.INSTANCE
						.executeScheduler(scheduler);
				return schedulerStatus;
			}
			if (enabled == scheduler.enabled)
				return schedulerStatus;
			scheduler.enabled = enabled;
			schedulerStatus.enabled = scheduler.enabled;
			SchedulerManager.INSTANCE.setScheduler(scheduler_name, scheduler);
			return schedulerStatus;
		} catch (WebApplicationException | IOException | SchedulerException
				| URISyntaxException | ServerException e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Response delete(String scheduler_name) {
		try {
			SchedulerManager.INSTANCE.deleteScheduler(scheduler_name);
			return Response.accepted().build();
		} catch (ServerException | SchedulerException e) {
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public SchedulerDefinition set(String scheduler_name,
			SchedulerDefinition scheduler) {
		try {
			return SchedulerManager.INSTANCE.setScheduler(scheduler_name,
					scheduler);
		} catch (IOException | SchedulerException e) {
			throw ServerException.getTextException(e);
		}
	}

}
