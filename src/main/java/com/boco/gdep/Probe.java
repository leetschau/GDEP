package com.boco.gdep;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Probe {
	private static final Logger logger = LoggerFactory.getLogger(Probe.class);
	public static final String REPORT_FILE = "DepResult.txt";

	private Map<String, Map<String, String>> report = new LinkedHashMap<String, Map<String, String>>();

	public static final String CONF_FILE_PATH = "dep.conf";

	public static final String OSTYPE = "ostype";
	public static final String CPU = "cpu";
	public static final String MEMORY = "memory";
	public static final String DISK = "disk";
	public static final String JDK = "jdk";
	public static final String FD = "fd";

	public Probe() {
		Map<String, String> basic = new LinkedHashMap<String, String>();
		basic.put(OSTYPE, "");
		basic.put(CPU, "");
		basic.put(MEMORY, "");
		basic.put(DISK, "");
		basic.put(JDK, "");
		basic.put(FD, "");
		report.put("basic", basic);
	}

	public void testSystem() throws ConfigurationException, IOException {
		final HierarchicalINIConfiguration probeConfs = new HierarchicalINIConfiguration(
				CONF_FILE_PATH);
		Set<String> sections = probeConfs.getSections();

		for (String sectionName : sections) {
			logger.debug("Get section: " + sectionName);
			if (!report.containsKey(sectionName)) {
				logger.error("Section name " + sectionName
						+ " is invalid. Correct it and run again.");
				System.exit(-1);
			}
			SubnodeConfiguration items = probeConfs.getSection(sectionName);
			Iterator<String> keys = items.getKeys();
			while (keys.hasNext()) {
				String key = keys.next();
				String value = items.getString(key);
				logger.debug("key: " + key + ", value: " + value);
				Map<String, String> result = (Map<String, String>) report
						.get(sectionName);
				if (!result.containsKey(key)) {
					logger.error("Check item " + key
							+ " is invalid. Correct it and run again.");
					System.exit(-1);
				}
				result.put(key, getCheckResult(key));
			}
		}
		String jsonText = JSONValue.toJSONString(report);
		FileUtils.writeStringToFile(new File(REPORT_FILE), jsonText);
		logger.info("Report has been written to result file successfully.");
	}

	private String getCheckResult(String key) {
		if (key.equals(OSTYPE)) {
			return System.getProperty("os.name") + " v"
					+ System.getProperty("os.version") + " "
					+ System.getProperty("os.arch");
		} else if (key.equals(CPU)) {
			String cpuModel = runLinuxShell("cat /proc/cpuinfo", "model name.*");
			return cpuModel.split(": ")[1];
		} else if (key.equals(MEMORY)) {
			String allMem = runLinuxShell("free -m", "Mem:.*");
			return allMem.split(" +")[1] + "MB";
		} else if (key.equals(DISK)) {
			String allDisk = runLinuxShell("df -m .", "/.*");
			return allDisk.split(" +")[1] + "MB";
		} else if (key.equals(JDK)) {
			return System.getProperty("java.vendor") + " "
					+ System.getProperty("java.version");
		} else if (key.equals(FD)) {
			String fileDesc = runLinuxShell("cat /proc/sys/fs/file-max", null);
			return fileDesc;
		} else {
			return "";
		}
	}

	private String runLinuxShell(String cmd, String pattern) {
		Process process;
		try {
			process = Runtime.getRuntime().exec(cmd);
			InputStreamReader ir = new InputStreamReader(
					process.getInputStream());
			LineNumberReader input = new LineNumberReader(ir);
			if (pattern == null) {
				return input.readLine();
			}
			String line;
			while ((line = input.readLine()) != null) {
				if (line.matches(pattern)) {
					return line;
				} else {
					continue;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws ConfigurationException,
			IOException {
		Probe probe = new Probe();
		probe.testSystem();
	}
}