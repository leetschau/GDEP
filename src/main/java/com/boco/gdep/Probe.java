package com.boco.gdep;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

public class Probe {

	public static final String CONF_FILE_PATH = "src/test/resources/dep.conf";

	public void testSystem() throws ConfigurationException {
		HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration(
				CONF_FILE_PATH);
		Set setOfSections = iniConfObj.getSections();
		System.out.println("sets of sections:" + setOfSections);
		Iterator sectionNames = setOfSections.iterator();

		while (sectionNames.hasNext()) {
			String sectionName = sectionNames.next().toString();
			System.out.println("Section name: " + sectionName);
			HierarchicalINIConfiguration iniObj = null;
			SubnodeConfiguration sObj = iniConfObj.getSection(sectionName);
			Iterator it1 = sObj.getKeys();

			while (it1.hasNext()) {
				// Get element
				Object key = it1.next();
				System.out.print("Key " + key.toString() + " Value "
						+ sObj.getString(key.toString()) + "\n");
			}
		}
		// Properties p = new Properties();
		// p.load(new FileReader(CONF_FILE_PATH));
		// Enumeration<?> enumeration = p.propertyNames();
		// while (enumeration.hasMoreElements()) {
		// String key = (String) enumeration.nextElement();
		// String value = p.getProperty(key);
		// System.out.println(key + "=" + value);
		// }
		// System.out.println(p.propertyNames());
		// String ostype = System.getProperty("os.name") + "-"
		// + System.getProperty("os.arch");

	}

	public static void main(String[] args) throws ConfigurationException {
		Probe probe = new Probe();
		probe.testSystem();
	}
}