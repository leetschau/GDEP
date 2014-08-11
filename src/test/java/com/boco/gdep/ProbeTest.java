package com.boco.gdep;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

public class ProbeTest {

	@Test
	public void testrun() throws ConfigurationException {
		Probe probe = new Probe();
		probe.testSystem();
		// fail("aaa");
	}
}
