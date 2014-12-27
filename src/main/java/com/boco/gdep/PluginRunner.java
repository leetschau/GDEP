package com.boco.gdep;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginRunner {

	private static final Logger logger = LoggerFactory
			.getLogger(PluginRunner.class);
	public static final String SHELL_SECTION_NAME = "shell";
	public static final String JAVA_SECTION_NAME = "java";
	public static final String USER_PLUGIN_PATH = "plugins";
	public static final String SHELL_LOG = "user_shell.log";
	public static final String JAVA_LOG = "user_java.log";

	public static final String USER_SCRIPT_PATH = USER_PLUGIN_PATH
			+ "/user.script";

	public static final String USER_CLASSPATH = USER_PLUGIN_PATH + "/lib/*";

	private HierarchicalINIConfiguration userScirpt;

	public void runUserScript() throws ConfigurationException, IOException {
		userScirpt = new HierarchicalINIConfiguration(USER_SCRIPT_PATH);
		Set<String> sections = userScirpt.getSections();

		List<String> shellResult = new ArrayList<String>();
		List<String> javaResult = new ArrayList<String>();

		for (String sectionName : sections) {
			logger.debug("Get section: " + sectionName);

			SubnodeConfiguration items = userScirpt.getSection(sectionName);
			Iterator<String> keys = items.getKeys();
			while (keys.hasNext()) {
				String key = keys.next();
				// Apache commons configuration parse "." in keys as ".."
				// so fix it:
				String rightName = key.replace("..", ".");
				logger.debug("Get key: " + rightName);
				if (sectionName.equals(SHELL_SECTION_NAME)) {
					shellResult.add(runShell(rightName));
				} else if (sectionName.equals(JAVA_SECTION_NAME)) {
					javaResult.add(runJavaProgram(rightName));
				} else {
					logger.error("Unknown section name: " + sectionName);
				}
			}
		}
		if (shellResult.size() > 0) {
			FileUtils.writeStringToFile(
					new File(SHELL_LOG),
					StringUtils.join(shellResult,
							System.getProperty("line.separator")));
		}
		if (javaResult.size() > 0) {
			FileUtils.writeStringToFile(
					new File(JAVA_LOG),
					StringUtils.join(javaResult,
							System.getProperty("line.separator")));
		}
	}

	private String runShell(String cmd) {
		logger.info("Running user command: " + cmd);
		String output = "";
		try {
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				logger.debug(line);
				output += line + System.getProperty("line.separator");
			}
			process.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return output;
	}

	private String runJavaProgram(String mainClass) {
		logger.info("Running java program in class: " + mainClass);
		String output = "";
		try {
			ProcessBuilder pb = new ProcessBuilder("java", "-cp",
					USER_CLASSPATH, mainClass);
			pb.redirectErrorStream(true);
			Process process = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				logger.debug(line);
				output += line + System.getProperty("line.separator");
			}
			process.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return output;
	}
}
