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

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.qwazr.utils.server.ServerException;

@DisallowConcurrentExecution
public class SchedulerJob implements Job {

	public SchedulerJob() {

	}

	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		String scheduler_name = context.getJobDetail().getKey().getName();
		try {
			SchedulerManager.INSTANCE.executeScheduler(scheduler_name);
		} catch (ServerException | IOException | URISyntaxException e) {
			throw new JobExecutionException(e);
		}
	}
}
