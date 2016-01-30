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
import com.qwazr.utils.file.TrackedDirectory;
import com.qwazr.utils.file.TrackedInterface;
import com.qwazr.utils.json.JsonMapper;
import com.qwazr.utils.server.ServerException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.quartz.impl.DirectSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.BiConsumer;

public class SchedulerManager implements TrackedInterface.FileChangeConsumer {

	public static final String SERVICE_NAME_SCHEDULER = "schedulers";

	private static final Logger logger = LoggerFactory.getLogger(SchedulerManager.class);

	static SchedulerManager INSTANCE = null;

	public static synchronized Class<? extends SchedulerServiceInterface> load(TrackedDirectory etcTracker,
			Set<String> confSet, int maxThreads) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		try {
			INSTANCE = new SchedulerManager(etcTracker, confSet, maxThreads);
			etcTracker.register(INSTANCE);
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

	private final Scheduler globalScheduler;
	private final Map<String, List<ScriptRunStatus>> schedulerStatusMap;
	private final LockUtils.ReadWriteLock statusMapLock;
	private final TrackedDirectory etcTracker;
	private final Set<String> confSet;

	private final LockUtils.ReadWriteLock mapLock;
	private final Map<File, Map<String, SchedulerDefinition>> schedulerFileMap;
	private volatile Map<String, SchedulerDefinition> schedulerMap;

	private SchedulerManager(TrackedDirectory etcTracker, Set<String> confSet, int maxThreads)
			throws IOException, SchedulerException, ServerException {
		this.confSet = confSet;
		this.etcTracker = etcTracker;
		statusMapLock = new LockUtils.ReadWriteLock();
		mapLock = new LockUtils.ReadWriteLock();
		schedulerMap = null;
		schedulerFileMap = new HashMap<>();
		schedulerStatusMap = new HashMap<String, List<ScriptRunStatus>>();
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
		etcTracker.check();
		final TreeMap<String, String> map = new TreeMap<String, String>();
		final Map<String, SchedulerDefinition> scMap = schedulerMap;
		if (scMap == null)
			return map;
		scMap.forEach((name, schedulerDef) -> map.put(name, ClusterManager.INSTANCE.myAddress + "/schedulers/" + name));
		return map;
	}

	SchedulerDefinition getScheduler(String scheduler_name) throws IOException, ServerException {
		etcTracker.check();
		final Map<String, SchedulerDefinition> scMap = schedulerMap;
		SchedulerDefinition schedulerDefinition = scMap == null ? null : scMap.get(scheduler_name);
		if (schedulerDefinition == null)
			throw new ServerException(Status.NOT_FOUND, "Scheduler not found: " + scheduler_name);
		return schedulerDefinition;
	}

	List<ScriptRunStatus> getStatusList(String scheduler_name) throws IOException, ServerException {
		statusMapLock.r.lock();
		try {
			return schedulerStatusMap.get(scheduler_name);
		} finally {
			statusMapLock.r.unlock();
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

	@Override
	public void accept(TrackedInterface.ChangeReason changeReason, File jsonFile) {
		if (confSet != null) {
			String filebase = FilenameUtils.removeExtension(jsonFile.getName());
			if (!confSet.contains(filebase))
				return;
		}
		String extension = FilenameUtils.getExtension(jsonFile.getName());
		if (!"json".equals(extension))
			return;
		switch (changeReason) {
		case UPDATED:
			loadSchedulerConf(jsonFile);
			break;
		case DELETED:
			unloadSchedulerConf(jsonFile);
			break;
		}
	}

	private void loadSchedulerConf(File jsonFile) {
		try {
			final SchedulerConfiguration schedulerConfiguration = JsonMapper.MAPPER
					.readValue(jsonFile, SchedulerConfiguration.class);

			if (schedulerConfiguration == null || schedulerConfiguration.schedulers == null)
				return;

			if (logger.isInfoEnabled())
				logger.info("Load Scheduler configuration file: " + jsonFile.getAbsolutePath());

			mapLock.w.lock();
			try {
				schedulerFileMap.put(jsonFile, schedulerConfiguration.schedulers);
				buildSchedulerMap();
			} finally {
				mapLock.w.unlock();
			}

		} catch (IOException | SchedulerException e) {
			if (logger.isErrorEnabled())
				logger.error(e.getMessage(), e);
		}
	}

	private void unloadSchedulerConf(File jsonFile) {
		mapLock.w.lock();
		try {
			final Map<String, SchedulerDefinition> schedulerDefMap = schedulerFileMap.remove(jsonFile);
			if (schedulerDefMap == null)
				return;
			if (logger.isInfoEnabled())
				logger.info("Unload Scheduler configuration file: " + jsonFile.getAbsolutePath());
			buildSchedulerMap();
		} catch (SchedulerException e) {
			if (logger.isErrorEnabled())
				logger.error(e.getMessage(), e);
		} finally {
			mapLock.w.unlock();
		}
	}

	private void buildSchedulerMap() throws SchedulerException {
		synchronized (globalScheduler) {
			globalScheduler.clear();
		}
		final Map<String, SchedulerDefinition> map = new HashMap<>();
		schedulerFileMap.forEach((file, schedulerDefMap) -> map.putAll(schedulerDefMap));
		final List<String> removeKeys = new ArrayList<>();

		// Remove the no more existing jobs status
		statusMapLock.r.lock();
		try {
			schedulerStatusMap.forEach(new BiConsumer<String, List<ScriptRunStatus>>() {
				@Override
				public void accept(String name, List<ScriptRunStatus> scriptRunStatuses) {
					if (!map.containsKey(name))
						removeKeys.add(name);
				}
			});
			removeKeys.forEach((name) -> schedulerStatusMap.remove(name));
		} finally {
			statusMapLock.r.unlock();
		}

		// Set the volatile map
		schedulerMap = map;

		// Reschedule the jobs
		schedulerMap.forEach(new BiConsumer<String, SchedulerDefinition>() {
			@Override
			public void accept(String name, SchedulerDefinition schedulerDefinition) {
				try {
					checkSchedulerCron(name, schedulerDefinition);
				} catch (SchedulerException e) {
					if (logger.isErrorEnabled())
						logger.error("Error on scheduler " + name + ": " + e.getMessage(), e);
				}
			}
		});
	}

}
