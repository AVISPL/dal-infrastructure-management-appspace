package com.avispl.symphony.dal.infrastructure.management.appspace;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.dal.infrastructure.management.appspace.common.utils.Util;
import com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregator.AggregatorProperty;

class AppspaceCloudCommunicatorTest {
	private AppspaceCloudCommunicator appspaceCloudCommunicator;
	private ExtendedStatistics extendedStatistics;

	@BeforeEach
	void setUp() throws Exception {
		appspaceCloudCommunicator = new AppspaceCloudCommunicator();
		appspaceCloudCommunicator.setHost("");
		appspaceCloudCommunicator.setPort(443);
		appspaceCloudCommunicator.setLogin("");
		appspaceCloudCommunicator.setPassword("");
		appspaceCloudCommunicator.init();
		appspaceCloudCommunicator.connect();

		appspaceCloudCommunicator.setLocationId("");
		this.extendedStatistics = (ExtendedStatistics) this.appspaceCloudCommunicator.getMultipleStatistics().get(0);
	}

	@AfterEach
	void destroy() throws Exception {
		appspaceCloudCommunicator.disconnect();
		appspaceCloudCommunicator.destroy();
	}

	@Test
	void testGetMultipleStatistics() throws Exception {
		//	Active the collection thread to collect the devices
		this.appspaceCloudCommunicator.retrieveMultipleStatistics();
		Util.delayExecution(60000L);
		this.extendedStatistics = (ExtendedStatistics) this.appspaceCloudCommunicator.getMultipleStatistics().get(0);
		Map<String, String> properties = this.extendedStatistics.getStatistics();

		Assertions.assertEquals(properties.size(), AggregatorProperty.values().length, "Statistics are not enough properties");
		Arrays.stream(AggregatorProperty.values()).forEach(property -> {
			Assertions.assertTrue(properties.containsKey(property.getName()), "Have no property: " + property.getName());
			Assertions.assertNotNull(properties.get(property.getName()), "Null value from property " + property.getName());
		});
	}

	@Test
	void testRetrieveMultipleStatistics() throws Exception {
		//	Active the collection thread for the first time
		this.appspaceCloudCommunicator.retrieveMultipleStatistics();
		Util.delayExecution(60000L);

		//	Start verify before active the collection thread the next time
		for (int i = 0; i < 2; i++) {
			List<AggregatedDevice> aggregatedDevices = this.appspaceCloudCommunicator.retrieveMultipleStatistics();
			aggregatedDevices.forEach(aggregatedDevice -> {
				Assertions.assertNotNull(aggregatedDevice.getDeviceId(), "Aggregated device's ID is null");
				Assertions.assertNotNull(aggregatedDevice.getDeviceName(), "Aggregated device's name is null");
				Assertions.assertNotNull(aggregatedDevice.getDeviceOnline(), "Aggregated device's online is null");
				Assertions.assertNotNull(aggregatedDevice.getProperties(), "Aggregated device's properties is null");
			});
			Util.delayExecution(60000L);
		}
	}
}
