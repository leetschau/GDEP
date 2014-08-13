package com.boco.gdep;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Probe {
	private static final Logger logger = LoggerFactory.getLogger(Probe.class);

	public static final String CONF_FILE_PATH = "dep.conf";
	public static final String OS_TYPE = "ostype";
	public static final String CPU_FREQ = "freq";
	public static final String MEMORY = "memory";
	public static final String DISK = "disk";
	public static final String JDK = "jdk";
	public static final String FD = "fd";

	public void testSystem() throws ConfigurationException {
		final HierarchicalINIConfiguration probeConfs = new HierarchicalINIConfiguration(
				CONF_FILE_PATH);
		Set<String> sections = probeConfs.getSections();

		for (String sectionName : sections) {
			logger.debug("Get section: " + sectionName);
			SubnodeConfiguration items = probeConfs.getSection(sectionName);
			Iterator<String> keys = items.getKeys();
			while (keys.hasNext()) {
				String key = keys.next();
				String value = items.getString(key);
				logger.debug("key: " + key + ", value: " + value);
				if (key == OS_TYPE) {

				}
			}
		}
	}

	public static void main(String[] args) throws ConfigurationException {
		Probe probe = new Probe();
		probe.testSystem();
	}
}