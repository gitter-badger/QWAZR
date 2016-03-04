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
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Mojo(name = "start", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class QwazrStartMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	@Parameter(property = "qwazr_data")
	private String data_directory;

	@Parameter(property = "qwazr.listen_addr")
	private String listen_addr;

	@Parameter(property = "qwazr.public_addr")
	private String public_addr;

	@Parameter(property = "qwazr.webapp_port")
	private Integer webapp_port;

	@Parameter(property = "qwazr.webservice_port")
	private Integer webservice_port;

	@Parameter(property = "qwazr.webapp_realm")
	private String webapp_realm;

	@Parameter(property = "qwazr.webservice_realm")
	private String webservice_realm;

	@Parameter(property = "qwazr.etc")
	private List<String> etc;

	@Parameter(property = "qwazr.services")
	private List<QwazrConfiguration.ServiceEnum> services;

	@Parameter(property = "qwazr.scheduler_max_threads")
	private Integer scheduler_max_threads;

	@Parameter(property = "qwazr.groups")
	private List<String> groups;

	@Parameter(property = "qwazr.daemon")
	private Boolean daemon;

	public void execute() throws MojoExecutionException, MojoFailureException {
		final Log log = getLog();
		log.info("Starting QWAZR");
		final Launcher launcher = new Launcher();

		try {
			if (daemon == null || !daemon)
				launcher.startEmbedded(log);
			else
				launcher.startAsDaemon(log);
		} catch (Exception e) {
			throw new MojoFailureException("Cannot start QWAZR", e);
		}
	}

	private class Launcher {

		private final Map<String, String> parameters = new HashMap<>();

		private Launcher() {
			setParameter(ServerConfiguration.VariablesEnum.QWAZR_DATA, data_directory);
			setParameter(ServerConfiguration.VariablesEnum.LISTEN_ADDR, listen_addr);
			setParameter(ServerConfiguration.VariablesEnum.PUBLIC_ADDR, public_addr);
			setParameter("WEBAPP_PORT", webapp_port);
			setParameter("WEBSERVICE_PORT", webservice_port);
			setParameter("WEBAPP_REALM", webapp_realm);
			setParameter("WEBSERVICE_REALM", webservice_realm);
		}

		private void setParameter(Enum<?> key, Object value) {
			setParameter(key.name(), value);
		}

		private void setParameter(String key, Object value) {
			if (value == null)
				return;
			String str = value.toString();
			if (StringUtils.isEmpty(str))
				return;
			System.setProperty(key, str);
			parameters.put(key, str);
		}

		private String buildClassPass() throws DependencyResolutionRequiredException {
			final StringBuilder sb = new StringBuilder();

			// Build the runtime classpath
			List<String> classPathList = project.getRuntimeClasspathElements();
			classPathList.forEach(new Consumer<String>() {
				@Override
				public void accept(String path) {
					sb.append(path);
					sb.append(File.pathSeparatorChar);
				}
			});

			// Build the artifacts classpath
			Set<Artifact> artifacts = project.getArtifacts();
			if (artifacts != null)
				artifacts.forEach(new Consumer<Artifact>() {
					@Override
					public void accept(Artifact artifact) {
						sb.append(artifact.getFile().getPath());
						sb.append(File.pathSeparatorChar);
					}
				});
			return sb.toString();
		}

		private void startAsDaemon(final Log log) throws MojoFailureException, IOException, InterruptedException,
						DependencyResolutionRequiredException {

			final File javaBinFile = new File(System.getProperty("java.home"),
							File.separator + "bin" + File.separator + (SystemUtils.IS_OS_WINDOWS ?
											"java.exe" :
											"java"));
			if (!javaBinFile.exists())
				throw new MojoFailureException("Cannot find JAVA: " + javaBinFile);

			final String classpath = buildClassPass();
			parameters.put("CLASSPATH", classpath);

			final String className = Qwazr.class.getCanonicalName();
			final ProcessBuilder builder = new ProcessBuilder(javaBinFile.getCanonicalPath(), "-Dfile.encoding=UTF-8",
							className);

			builder.environment().putAll(parameters);
			builder.inheritIO();
			Process process = builder.start();
			log.info("QWAZR started (Daemon)");
			process.waitFor(5000, TimeUnit.MILLISECONDS);
		}

		private void startEmbedded(final Log log)
						throws ParseException, InstantiationException, IllegalAccessException, ServletException,
						IOException {
			Qwazr.start(new QwazrConfiguration(etc, services, groups, scheduler_max_threads));
			log.info("QWAZR started (Embedded)");
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
}
