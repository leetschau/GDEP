package com.boco.gdep;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
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

	public static final String BASIC = "basic";
	public static final String PERF = "performance";
	public static final String CLIENT = "client";

	public static final String OSTYPE = "ostype";
	public static final String CPU = "cpu";
	public static final String MEMORY = "memory";
	public static final String DISK = "disk";
	public static final String JDK = "jdk";
	public static final String FD = "file-descriptor";
	public static final String NET_IO = "net_io";
	public static final String PKG_LOSS = "pkg_loss";
	public static final String CLIENT_USER = "user";
	public static final String CLIENT_IP = "ip";

	public static final String CPU_CALC = "cpu_calc";
	public static final String DISK_IO = "disk_io";

	public static final int PI_ROUNDS = 40000000;

	private HierarchicalINIConfiguration probeConfs;

	public Probe() {
		Map<String, String> basic = new LinkedHashMap<String, String>();
		basic.put(OSTYPE, "");
		basic.put(CPU, "");
		basic.put(MEMORY, "");
		basic.put(DISK, "");
		basic.put(JDK, "");
		basic.put(FD, "");
		report.put(BASIC, basic);

		Map<String, String> perf = new LinkedHashMap<String, String>();
		perf.put(CPU_CALC, "");
		perf.put(DISK_IO, "");
		perf.put(PKG_LOSS, "");
		perf.put(NET_IO, "");
		report.put(PERF, perf);
	}

	public void testSystem() throws ConfigurationException, IOException {
		probeConfs = new HierarchicalINIConfiguration(CONF_FILE_PATH);
		Set<String> sections = probeConfs.getSections();

		for (String sectionName : sections) {
			logger.debug("Get section: " + sectionName);
			if (sectionName.equals(CLIENT)) {
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

			Map<String, String> ipsection = new LinkedHashMap<String, String>();
			ipsection.put("ip", getClientInfo(false));
			report.put("Test Peer", ipsection);
		}
		String rawJsonText = JSONValue.toJSONString(report);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(rawJsonText);

		String jsonReport = gson.toJson(je);
		FileUtils.writeStringToFile(new File(REPORT_FILE), jsonReport);
		logger.info("Report has been written to result file successfully.Performance index");
	}

	private String getCheckResult(String key) throws IOException {

		if (key.equals(OSTYPE)) {
			return System.getProperty("os.name") + " v"
					+ System.getProperty("os.version") + " "
					+ System.getProperty("os.arch");
		} else if (key.equals(CPU)) {
			String cpuModel = runLinuxShell("lscpu", "CPU\\(s\\).*");
			return cpuModel.split(": +")[1];
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
		} else if (key.equals(CPU_CALC)) {
			return testCPUPerformance();
		} else if (key.equals(DISK_IO)) {
			String diskio = testDiskIOSpeed();
			return diskio;
		} else {
			return "";
		}
	}

	String getClientInfo(boolean withUser) {
		String user = probeConfs.getSection(CLIENT).getString(CLIENT_USER);
		String ip = probeConfs.getSection(CLIENT).getString(CLIENT_IP);
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

	public String testDiskIOSpeed() throws IOException {
		runLinuxShell("dd if=/dev/zero of=test bs=1M count=500", "copied");
		File afile = new File("test");
		File bfile = new File("test1");
		InputStream inStream = null;
		OutputStream outStream = null;
		long io_speed = 0;
		try {
			inStream = new FileInputStream(afile);
			outStream = new FileOutputStream(bfile);
			byte[] buffer = new byte[1024];
			int length;
			Thread.sleep(1000); // or the InputStream.read returns -1
			long start = System.nanoTime();

			while ((length = inStream.read(buffer)) > 0) {
				outStream.write(buffer, 0, length);
			}
			long estimatedTime = TimeUnit.NANOSECONDS.toSeconds(System
					.nanoTime() - start);
			io_speed = 500 / estimatedTime;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			inStream.close();
			outStream.close();
		}

		runLinuxShell("rm -rf test test1", null);
		return (io_speed + "MB/s");
	}

	public String testCPUPerformance() {
		long start = System.nanoTime();
		double res = 0;
		for (int i = 0; i < PI_ROUNDS; i++) {
			res = Math.pow(Math.PI, 100);
		}
		logger.debug("Pi value is " + res);
		long estimatedTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()
				- start);
		return String.valueOf(estimatedTime);
	}

	public static void main(String[] args) throws ConfigurationException,
			IOException {
		Probe probe = new Probe();
		probe.testSystem();
	}
}