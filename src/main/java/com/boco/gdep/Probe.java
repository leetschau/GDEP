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

	public static final String STANDARD_FILE_PATH = "DepStandard.txt";
	public static final String CONF_FILE_PATH = "dep.conf";

	public static final String BASIC = "basic";
	public static final String PERF = "performance";
	public static final String CLIENT = "client";

	public static final String OSTYPE = "ostype";
	public static final String CPU = "cpu";
	public static final String MEMORY = "memory";
	public static final String DISK = "disk";
	public static final String JDK = "jdk";
	public static final String FD = "file_descriptor";
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
		File template = new File(CONF_FILE_PATH);
		File stdReport = new File(STANDARD_FILE_PATH);
		if (!template.exists()) {
			logger.error("Neither config or standard file exists!");
			System.exit(-1);
		}
		probeConfs = new HierarchicalINIConfiguration(CONF_FILE_PATH);
		if (stdReport.exists()) {
			logger.info("Running a relative test based on "
					+ stdReport.getAbsolutePath());
			relativeTest(stdReport);
		} else {
			logger.info("Running a standard test.");
			standardTest();
		}

		String rawJsonText = JSONValue.toJSONString(report);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(rawJsonText);

		String jsonReport = gson.toJson(je);
		FileUtils.writeStringToFile(new File(REPORT_FILE), jsonReport);
		logger.info("Report has been written to result file successfully.");
	}

	private void standardTest() throws ConfigurationException, IOException {
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
				logger.debug("Test item: " + key);
				Map<String, String> result = (Map<String, String>) report
						.get(sectionName);
				if (!result.containsKey(key)) {
					logger.error("Check item " + key
							+ " is invalid. Correct it and run again.");
					System.exit(-1);
				}
				result.put(key, getCheckResult(key, null));
			}

			Map<String, String> ipsection = new LinkedHashMap<String, String>();
			ipsection.put("ip", getClientInfo(false));
			report.put(CLIENT, ipsection);
		}
	}

	private void relativeTest(File stdReport) throws IOException {
		String jsonReport = FileUtils.readFileToString(stdReport);
		Map<String, Map<String, String>> standard_report = (Map<String, Map<String, String>>) JSONValue
				.parse(jsonReport);
		for (String sectionName : standard_report.keySet()) {
			logger.debug("Get section: " + sectionName);
			if (sectionName.equals(CLIENT)) {
				continue;
			}
			if (!report.containsKey(sectionName)) {
				logger.error("Section name " + sectionName
						+ " is invalid. Correct it and run again.");
				System.exit(-1);
			}
			Map<String, String> testsuite = standard_report.get(sectionName);
			for (String key : testsuite.keySet()) {
				String stdVal = testsuite.get(key);
				logger.debug("Test item: " + key + ", standard: " + stdVal);
				Map<String, String> result = (Map<String, String>) report
						.get(sectionName);
				if (!result.containsKey(key)) {
					logger.error("Check item " + key
							+ " is invalid. Correct it and run again.");
					System.exit(-1);
				}
				result.put(key, getCheckResult(key, stdVal));
			}

			Map<String, String> ipsection = new LinkedHashMap<String, String>();
			ipsection.put("ip", getClientInfo(false));
			report.put(CLIENT, ipsection);
		}
	}

	private String getCheckResult(String key, String stdVal) throws IOException {
		if (key.equals(OSTYPE)) {
			String os = System.getProperty("os.name") + " v"
					+ System.getProperty("os.version") + " "
					+ System.getProperty("os.arch");
			return buildTextResult(os, stdVal);
		} else if (key.equals(CPU)) {
			String coreNum = runLinuxShell("lscpu", "CPU\\(s\\).*")
					.split(": +")[1];
			String cpuInfo = runLinuxShell("cat /proc/cpuinfo", "model name.*")
					.split(": ")[1] + " * " + coreNum;
			return buildTextResult(cpuInfo, stdVal);
		} else if (key.equals(MEMORY)) {
			String allMem = runLinuxShell("free -m", "Mem:.*").split(" +")[1];
			return buildNumberResult(allMem, "MB", stdVal);
		} else if (key.equals(DISK)) {
			String allDisk = runLinuxShell("df -Pm .", "/.*").split(" +")[1];
			return buildNumberResult(allDisk, "MB", stdVal);
		} else if (key.equals(JDK)) {
			String jdkInfo = System.getProperty("java.vendor") + " "
					+ System.getProperty("java.version");
			return buildTextResult(jdkInfo, stdVal);
		} else if (key.equals(FD)) {
			String fileDesc = runLinuxShell("cat /proc/sys/fs/file-max", null);
			return buildNumberResult(fileDesc, "", stdVal);
		} else if (key.equals(NET_IO)) {
			runLinuxShell("dd if=/dev/zero of=test bs=1M count=100", "copied");
			String copyfile = "scp test " + getClientInfo(true) + ":~/";
			long start = System.nanoTime();
			runLinuxShell(copyfile, "100%");
			long estimatedTime = TimeUnit.NANOSECONDS.toSeconds(System
					.nanoTime() - start);
			long io_speed = 100 / estimatedTime;
			runLinuxShell("rm -rf test", null);
			return buildNumberResult(Long.toString(io_speed), "MB/s", stdVal);
		} else if (key.equals(PKG_LOSS)) {
			String pkgloss = runLinuxShell(
					"ping -c 10 " + getClientInfo(false), ".*packet.*");
			return buildTextResult(pkgloss.split(",")[2].split(" ")[1], stdVal);
		} else if (key.equals(CPU_CALC)) {
			return buildNumberResult(testCPUPerformance(), "", stdVal);
		} else if (key.equals(DISK_IO)) {
			String diskio = testDiskIOSpeed();
			return buildNumberResult(diskio, "MB/s", stdVal);
		} else {
			return "";
		}
	}

	private String buildNumberResult(String item, String unit, String stdVal) {
		String res = item + unit;
		if (stdVal == null) {
			return res;
		}
		if (res.equals(stdVal)) {
			return "match: " + res;
		} else {
			float thishost = Float.parseFloat(item);
			float standard = Float.parseFloat(stdVal.replaceAll("\\D+", ""));
			String compResult = "This host: " + thishost + unit
					+ "; Template: " + stdVal;
			if (standard == 0) {
				return compResult;
			}
			float ratio = thishost / standard * 100;
			return ratio + "%. " + compResult;
		}
	}

	private String buildTextResult(String info, String stdVal) {
		if (stdVal == null) {
			return info;
		}
		if (info.equals(stdVal)) {
			return "match: " + info;
		} else {
			return "mismatch. This host: " + info + "; Template: " + stdVal;
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
		return Long.toString(io_speed);
	}

	public String testCPUPerformance() {
		long start = System.nanoTime();
		double res = 0;
		for (int i = 0; i < PI_ROUNDS; i++) {
			res = Math.pow(Math.PI, 100);
		}
		logger.debug("Result is " + res);
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