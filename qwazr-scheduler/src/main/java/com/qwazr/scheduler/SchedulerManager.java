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

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.scripts.ScriptManager;
import com.qwazr.scripts.ScriptRunStatus;
import com.qwazr.utils.LockUtils;
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
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;

public class SchedulerManager {

	public static final String SERVICE_NAME_SCHEDULER = "schedulers";

	private static final Logger logger = LoggerFactory.getLogger(SchedulerManager.class);

	static SchedulerManager INSTANCE = null;

	public static synchronized Class<? extends SchedulerServiceInterface> load(File directory, int maxThreads)
			throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		try {
			INSTANCE = new SchedulerManager(directory, maxThreads);
			return SchedulerServiceImpl.class;
		} catch (ServerException | SchedulerException e) {
			throw new RuntimeException(e);
		}
	}

	public static SchedulerManager getInstance() {
		if (INSTANCE == null)
			throw new RuntimeException("The scheduler service is not enabled");
		return INSTANCE;
	}

	private final File schedulersDirectory;
	private final Scheduler globalScheduler;
	private final TreeMap<String, List<ScriptRunStatus>> schedulerStatusMap;
	private final LockUtils.ReadWriteLock statusMapLock;

	private SchedulerManager(File rootDirectory, int maxThreads)
			throws IOException, SchedulerException, ServerException {
		schedulersDirectory = new File(rootDirectory, SERVICE_NAME_SCHEDULER);
		if (!schedulersDirectory.exists())
			schedulersDirectory.mkdir();

		statusMapLock = new LockUtils.ReadWriteLock();
		schedulerStatusMap = new TreeMap<String, List<ScriptRunStatus>>();
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

	List<ScriptRunStatus> getStatusList(String scheduler_name) throws IOException, ServerException {
		statusMapLock.r.lock();
		try {
			return schedulerStatusMap.get(scheduler_name);
		} finally {
			statusMapLock.r.unlock();
		}
	}

	void deleteScheduler(String scheduler_name) throws ServerException, SchedulerException {
		synchronized (globalScheduler) {
			globalScheduler.deleteJob(new JobKey(scheduler_name));
		}
		getSchedulerFile(scheduler_name).delete();
		statusMapLock.w.lock();
		try {
			schedulerStatusMap.remove(scheduler_name);
		} finally {
			statusMapLock.w.unlock();
		}
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

	List<ScriptRunStatus> executeScheduler(String scheduler_name, SchedulerDefinition scheduler)
			throws IOException, ServerException, URISyntaxException {
		ClusterManager clusterManager = ClusterManager.INSTANCE;
		if (clusterManager.isCluster()) {
			if (!clusterManager.isLeader(SERVICE_NAME_SCHEDULER, null))
				return Collections.emptyList();
		}
		if (logger.isInfoEnabled())
			logger.info("execute " + scheduler_name + " / " + scheduler.script_path);
		long startTime = System.currentTimeMillis();
		List<ScriptRunStatus> statusList = ScriptManager.getInstance().getNewClient(scheduler.group, null)
				.runScriptVariables(scheduler.script_path, false, scheduler.group, scheduler.timeout, scheduler.rule,
						scheduler.variables);
		if (statusList != null) {
			statusList = ScriptRunStatus.cloneSchedulerResultList(statusList, startTime);
			statusMapLock.w.lock();
			try {
				schedulerStatusMap.put(scheduler_name, statusList);
			} finally {
				statusMapLock.w.unlock();
			}
		}
		return statusList;
	}

	List<ScriptRunStatus> executeScheduler(String scheduler_name)
			throws IOException, ServerException, URISyntaxException {
		return executeScheduler(scheduler_name, getScheduler(scheduler_name));
	}
}
