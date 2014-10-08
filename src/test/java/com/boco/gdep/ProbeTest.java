package com.boco.gdep;

import static com.boco.gdep.Probe.REPORT_FILE;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONValue;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ProbeTest {

	@Test
	public void testReportCreation() throws ConfigurationException, IOException {
		Probe probe = new Probe();
		probe.testSystem();
	}

	@Test
	public void testParseReport() throws IOException {
		String jsonReport = FileUtils.readFileToString(new File(REPORT_FILE));
		Map<String, Map<String, String>> report = (Map<String, Map<String, String>>) JSONValue
				.parse(jsonReport);
		Map<String, String> basic = report.get("basic");
		for (String key : basic.keySet()) {
			System.out.println("key " + key + ": " + basic.get(key));
		}
	}

	@Test
	public void testJsonPrettyPrint() throws IOException {
		String jsonReport = FileUtils.readFileToString(new File(REPORT_FILE));
		System.out.println("Origin:");
		System.out.println(jsonReport);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(jsonReport);
		String prettyJsonString = gson.toJson(je);
		System.out.println("New:");
		System.out.println(prettyJsonString);
	}

	@Test
	public void testElapsedTime() {
		Probe probe = new Probe();
		long start = System.nanoTime();
		probe.runLinuxShell("time vmstat 3 2", ".*total.*");
		long estimatedTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()
				- start);
		long io_speed = 11 / estimatedTime;
		System.out.println("speed is: " + io_speed);
	}

	@Test
	public void extractPkgLoss() {
		String raw = "10 packets transmitted, 10 received, 0% packet loss, time 8996ms";
		String pkgLoss = raw.split(",")[2];
		String plp = pkgLoss.split(" ")[1];
		System.out.println(plp);
	}

	@Test
	public void extractNumbers() {
		String stdVal = "328";
		String numPart = stdVal.replaceAll("\\D+", "");
		assertEquals(numPart, stdVal);
		String numPart2 = (stdVal + "MB/s").replaceAll("\\D+", "");
		assertEquals(numPart2, stdVal);
	}

	@Test
	public void extractCPU() {
		String cpuModel = "model name	: Intel(R) Core(TM) i5-3210M CPU @ 2.50GHz";
		String model = "Intel(R) Core(TM) i5-3210M CPU @ 2.50GHz";
		assertEquals(model, cpuModel.split(": ")[1]);
	}
}