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

import com.qwazr.scripts.ScriptRunStatus;
import com.qwazr.utils.server.ServerException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
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
			SchedulerDefinition schedulerDef = SchedulerManager.INSTANCE.getScheduler(scheduler_name);
			List<ScriptRunStatus> statusList = SchedulerManager.INSTANCE.getStatusList(scheduler_name);
			if (action == null)
				return new SchedulerStatus(schedulerDef, statusList);
			Boolean enabled = null;
			switch (action) {
			case enable:
				enabled = true;
				break;
			case disable:
				enabled = false;
				break;
			case run:
				return new SchedulerStatus(schedulerDef,
						SchedulerManager.INSTANCE.executeScheduler(scheduler_name, schedulerDef));
			}
			if (enabled == schedulerDef.enabled)
				return new SchedulerStatus(schedulerDef, statusList);
			schedulerDef.enabled = enabled;
			SchedulerManager.INSTANCE.setScheduler(scheduler_name, schedulerDef);
			return new SchedulerStatus(schedulerDef, statusList);
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
