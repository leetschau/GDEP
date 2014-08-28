package com.boco.gdep;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Probe {
	private static final Logger logger = LoggerFactory.getLogger(Probe.class);
	public static final String REPORT_FILE = "DepResult.txt";

	private Map<String, Map<String, String>> report = new LinkedHashMap<String, Map<String, String>>();

	public static final String CONF_FILE_PATH = "dep.conf";

	public static final String SEC_BASIC = "basic";
	public static final String SEC_PERF = "performance";
	public static final String SEC_CLIENT = "client";

	public static final String OSTYPE = "ostype";
	public static final String CPU = "cpu";
	public static final String MEMORY = "memory";
	public static final String DISK = "disk";
	public static final String JDK = "jdk";
	public static final String FD = "fd";
	public static final String NET_IO = "net_io";
	public static final String PKG_LOSS = "pkg_loss";
	public static final String CLIENT_USER = "user";
	public static final String CLIENT_IP = "ip";

	private HierarchicalINIConfiguration probeConfs;

	public Probe() {
		Map<String, String> basic = new LinkedHashMap<String, String>();
		basic.put(OSTYPE, "");
		basic.put(CPU, "");
		basic.put(MEMORY, "");
		basic.put(DISK, "");
		basic.put(JDK, "");
		basic.put(FD, "");
		report.put(SEC_BASIC, basic);

		Map<String, String> perf = new LinkedHashMap<String, String>();
		perf.put(NET_IO, "");
		perf.put(PKG_LOSS, "");
		report.put(SEC_PERF, perf);
	}

	public void testSystem() throws ConfigurationException, IOException {
		probeConfs = new HierarchicalINIConfiguration(CONF_FILE_PATH);
		Set<String> sections = probeConfs.getSections();

		for (String sectionName : sections) {
			logger.debug("Get section: " + sectionName);
			if (sectionName.equals(SEC_CLIENT)) {
				continue;
			}
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
		String rawJsonText = JSONValue.toJSONString(report);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(rawJsonText);
		String jsonReport = gson.toJson(je);
		FileUtils.writeStringToFile(new File(REPORT_FILE), jsonReport);
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
			String allDisk = runLinuxShell("df -Pm .", "/.*");
			return allDisk.split(" +")[1] + "MB";
		} else if (key.equals(JDK)) {
			return System.getProperty("java.vendor") + " "
					+ System.getProperty("java.version");
		} else if (key.equals(FD)) {
			String fileDesc = runLinuxShell("cat /proc/sys/fs/file-max", null);
			return fileDesc;
		} else if (key.equals(NET_IO)) {
			runLinuxShell("dd if=/dev/zero of=test bs=1M count=100", "copied");
			String copyfile = "scp test " + getClientInfo(true) + ":~/";
			long start = System.nanoTime();
			runLinuxShell(copyfile, "100%");
			long estimatedTime = TimeUnit.NANOSECONDS.toSeconds(System
					.nanoTime() - start);
			long io_speed = 100 / estimatedTime;
			runLinuxShell("rm -rf test", null);
			return io_speed + "MB/s";
		} else if (key.equals(PKG_LOSS)) {
			String pkgloss = runLinuxShell(
					"ping -c 10 " + getClientInfo(false), ".*packet.*");
			return pkgloss.split(",")[2].split(" ")[1];
		} else {
			return "";
		}
	}

	String getClientInfo(boolean withUser) {
		String user = probeConfs.getSection(SEC_CLIENT).getString(CLIENT_USER);
		String ip = probeConfs.getSection(SEC_CLIENT).getString(CLIENT_IP);
		if (withUser) {
			return user + "@" + ip;
		} else {
			return ip;
		}
	}

	String runLinuxShell(String cmd, String pattern) {
		Process process;
		String result = null;
		try {
			process = Runtime.getRuntime().exec(cmd);
			InputStreamReader ir = new InputStreamReader(
					process.getInputStream());
			LineNumberReader input = new LineNumberReader(ir);
			if (pattern == null) {
				result = input.readLine();
				logger.debug("Linux stdout: " + result);
				return result;
			}
			String line;
			while ((line = input.readLine()) != null) {
				logger.debug("Linux stdout: " + line);
				if (line.matches(pattern)) {
					result = line;
				} else {
					continue;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void main(String[] args) throws ConfigurationException,
			IOException {
		Probe probe = new Probe();
		probe.testSystem();
	}
}