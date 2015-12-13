/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.job.JobServer;
import com.qwazr.job.script.ScriptManager;
import com.qwazr.job.script.ScriptRunStatus;
import com.qwazr.utils.json.JsonMapper;
import com.qwazr.utils.server.ServerException;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.quartz.impl.DirectSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.TimeZone;
import java.util.TreeMap;

public class SchedulerManager {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerManager.class);

    public static volatile SchedulerManager INSTANCE = null;

    public static void load(File directory, int maxThreads) throws IOException, SchedulerException, ServerException {
	if (INSTANCE != null)
	    throw new IOException("Already loaded");
	INSTANCE = new SchedulerManager(directory, maxThreads);
    }

    private final File schedulersDirectory;
    private final Scheduler globalScheduler;

    private SchedulerManager(File rootDirectory, int maxThreads) throws IOException, SchedulerException,
	    ServerException {
	schedulersDirectory = new File(rootDirectory, JobServer.SERVICE_NAME_SCHEDULER);
	if (!schedulersDirectory.exists())
	    schedulersDirectory.mkdir();

	DirectSchedulerFactory schedulerFactory = DirectSchedulerFactory.getInstance();
	schedulerFactory.createVolatileScheduler(maxThreads);
	globalScheduler = schedulerFactory.getScheduler();
	globalScheduler.start();
	Runtime.getRuntime().addShutdownHook(new Thread() {
	    @Override
	    public void run() {
		try {
		    globalScheduler.shutdown();
		} catch (SchedulerException e) {
		    logger.error(e.getMessage(), e);
		}
	    }
	});

	for (String scheduler_name : getSchedulers().keySet())
	    checkSchedulerCron(scheduler_name, getScheduler(scheduler_name));
    }

    TreeMap<String, String> getSchedulers() {
	File[] files = schedulersDirectory.listFiles((FileFilter) FileFileFilter.FILE);
	TreeMap<String, String> map = new TreeMap<String, String>();
	if (files == null)
	    return map;
	for (File file : files)
	    if (!file.isHidden())
		map.put(file.getName(), ClusterManager.INSTANCE.myAddress + "/schedulers/" + file.getName());
	return map;
    }

    private File getSchedulerFile(String scheduler_name) throws ServerException {
	File schedulerFile = new File(schedulersDirectory, scheduler_name);
	if (!schedulerFile.exists())
	    throw new ServerException(Status.NOT_FOUND, "Scheduler not found: " + scheduler_name);
	if (!schedulerFile.isFile())
	    throw new ServerException(Status.NOT_ACCEPTABLE, "Scheduler is not a file: " + scheduler_name);
	return schedulerFile;
    }

    SchedulerDefinition getScheduler(String scheduler_name) throws IOException, ServerException {
	return JsonMapper.MAPPER.readValue(getSchedulerFile(scheduler_name), SchedulerDefinition.class);
    }

    void deleteScheduler(String scheduler_name) throws ServerException, SchedulerException {
	synchronized (globalScheduler) {
	    globalScheduler.deleteJob(new JobKey(scheduler_name));
	}
	getSchedulerFile(scheduler_name).delete();
    }

    private void checkSchedulerCron(String scheduler_name, SchedulerDefinition scheduler) throws SchedulerException {
	JobDetail job = JobBuilder.newJob(SchedulerJob.class).withIdentity(scheduler_name).build();
	if (scheduler.enabled != null && scheduler.enabled) {
	    CronScheduleBuilder cronBuilder = CronScheduleBuilder.cronSchedule(scheduler.cron);
	    if (!StringUtils.isEmpty(scheduler.time_zone))
		cronBuilder.inTimeZone(TimeZone.getTimeZone(scheduler.time_zone));
	    TriggerBuilder<CronTrigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(scheduler_name)
		    .withSchedule(cronBuilder).forJob(job);
	    CronTrigger trigger = triggerBuilder.build();
	    synchronized (globalScheduler) {
		globalScheduler.scheduleJob(job, trigger);
	    }
	} else {
	    synchronized (globalScheduler) {
		globalScheduler.deleteJob(job.getKey());
	    }
	}
    }

    SchedulerDefinition setScheduler(String scheduler_name, SchedulerDefinition scheduler)
	    throws IOException, SchedulerException {
	File schedulerFile = new File(schedulersDirectory, scheduler_name);
	JsonMapper.MAPPER.writeValue(schedulerFile, scheduler);
	checkSchedulerCron(scheduler_name, scheduler);
	return scheduler;
    }

    ScriptRunStatus executeScheduler(SchedulerDefinition scheduler) throws IOException, ServerException,
	    URISyntaxException {
	logger.info("execute " + scheduler.script_path);
	return ScriptManager.INSTANCE.getNewClient(null).runScriptVariables(scheduler.script_path, scheduler.variables);
    }

    ScriptRunStatus executeScheduler(String scheduler_name) throws IOException, ServerException, URISyntaxException {
	return executeScheduler(getScheduler(scheduler_name));
    }
}
