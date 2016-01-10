/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.scheduler;

import com.qwazr.utils.server.ServerException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.TreeMap;

public class SchedulerServiceImpl implements SchedulerServiceInterface {

	private final static Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

	@Override
	public TreeMap<String, String> list() {
		return SchedulerManager.INSTANCE.getSchedulers();
	}

	@Override
	public SchedulerStatus get(String scheduler_name, ActionEnum action) {
		try {
			SchedulerDefinition scheduler = SchedulerManager.INSTANCE.getScheduler(scheduler_name);
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
				schedulerStatus.script_status = SchedulerManager.INSTANCE.executeScheduler(scheduler);
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
	public SchedulerDefinition set(String scheduler_name, SchedulerDefinition scheduler) {
		try {
			return SchedulerManager.INSTANCE.setScheduler(scheduler_name, scheduler);
		} catch (IOException | SchedulerException e) {
			throw ServerException.getTextException(e);
		}
	}

}
