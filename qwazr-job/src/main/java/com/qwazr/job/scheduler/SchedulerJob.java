/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
