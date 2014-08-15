package com.boco.gdep;

import static com.boco.gdep.Probe.REPORT_FILE;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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
}