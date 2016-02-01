/**
 * Copyright 2016 Emmanuel Keller / QWAZR
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
 */
package com.qwazr.mavenplugin;

import com.qwazr.Qwazr;
import com.qwazr.QwazrConfiguration;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.server.ServerConfiguration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

@Mojo(name = "start")
public class QwazrStartMojo extends AbstractMojo {

	@Parameter
	private String data_directory;

	@Parameter
	private String listen_addr;

	@Parameter
	private String public_addr;

	@Parameter
	private Integer webapp_port;

	@Parameter
	private Integer webservice_port;

	@Parameter
	private List<String> etc;

	@Parameter
	private List<QwazrConfiguration.ServiceEnum> services;

	@Parameter
	private Integer scheduler_max_threads;

	@Parameter
	private List<String> groups;

	private void setProperty(Enum<?> key, Object value) {
		if (value == null)
			return;
		String str = value.toString();
		if (StringUtils.isEmpty(str))
			return;
		System.setProperty(key.name(), str);
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		final Log log = getLog();
		log.info("Starting QWAZR");
		try {
			setProperty(ServerConfiguration.VariablesEnum.QWAZR_DATA, data_directory);
			setProperty(ServerConfiguration.VariablesEnum.LISTEN_ADDR, listen_addr);
			setProperty(ServerConfiguration.VariablesEnum.PUBLIC_ADDR, public_addr);
			setProperty(ServerConfiguration.VariablesEnum.WEBAPP_PORT, webapp_port);
			setProperty(ServerConfiguration.VariablesEnum.WEBSERVICE_PORT, webservice_port);
			Qwazr.start(new QwazrConfiguration(etc, services, groups, scheduler_max_threads));
		} catch (Exception e) {
			throw new MojoFailureException("Cannot start QWAZR", e);
		}
		log.info("QWAZR started");

		try {
			for (; ; )
				Thread.sleep(30000);
		} catch (InterruptedException e) {
			log.info("QWAZR interrupted");
		}
		log.info("Stopping QWAZR");
		Qwazr.stop();
	}
}
