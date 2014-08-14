package com.boco.gdep;

import static com.boco.gdep.Probe.REPORT_FILE;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONValue;
import org.junit.Test;

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
}