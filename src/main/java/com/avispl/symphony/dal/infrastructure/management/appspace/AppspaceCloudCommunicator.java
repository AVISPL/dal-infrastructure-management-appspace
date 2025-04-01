/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.avispl.symphony.api.common.error.NotAuthorizedException;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.api.dal.monitor.aggregator.Aggregator;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.infrastructure.management.appspace.common.constants.Constant;
import com.avispl.symphony.dal.infrastructure.management.appspace.common.constants.Endpoint;
import com.avispl.symphony.dal.infrastructure.management.appspace.common.utils.Util;
import com.avispl.symphony.dal.infrastructure.management.appspace.models.Authorization;
import com.avispl.symphony.dal.infrastructure.management.appspace.models.Device;
import com.avispl.symphony.dal.infrastructure.management.appspace.models.Property;
import com.avispl.symphony.dal.infrastructure.management.appspace.models.requests.AuthorizationReq;
import com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregated.GeneralProperty;
import com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregated.SettingProperty;
import com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregator.AggregatorProperty;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * AppspaceCloudCommunicator class
 * <br>
 * The {@code AppspaceCloudCommunicator} class is responsible for communicating
 * with the Appspace Cloud to retrieve and manage device statistics.
 * <br>
 * <h3>- Monitoring Aggregator Device:</h3>
 * <li>AdapterBuildDate</li>
 * <li>AdapterUptime</li>
 * <li>AdapterUptime(min)</li>
 * <li>AdapterVersion</li>
 * <li>LastMonitoringCycleDuration(s)</li>
 * <li>MonitoredDevicesTotal</li>
 *
 * <h3>- Monitoring Aggregated Device:</h3>
 * <h4>+ General properties</h4>
 * <li>ID</li>
 * <li>DeviceType</li>
 * <li>Name</li>
 * <li>LocationName</li>
 * <li>GroupName</li>
 * <li>Status</li>
 * <li>AppVersion</li>
 * <li>LastOnline(+UTC)</li>
 * <li>Tags</li>
 * <li>Licenses</li>
 * <li>ChannelName</li>
 * <li>PlaybackMode</li>
 * <li>PublishMode</li>
 * <li>AutoSync</li>
 * <li>SyncStatus</li>
 * <li>IPAddress</li>
 * <li>LowBandwidthMode</li>
 *
 * <h4>+ Setting properties</h3>
 * <li>ContentDownloadMaxSpeed</li>
 * <li>ContentDownloadCycle</li>
 * <li>RebootEvents</li>
 * <li>ScreenBurnMode</li>
 * <li>ScreenBurnTime</li>
 * <li>ContentMaxConcurrentDownloads</li>
 * <li>Responsive</li>
 * <li>PublicAppSize</li>
 * <li>PlayerHasConstellationToken</li>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
public class AppspaceCloudCommunicator extends RestCommunicator implements Aggregator, Monitorable {
	/**
	 * Internal runnable class responsible for periodically fetching data from the Appspace Cloud.
	 */
	class AppspaceCloudDataLoader implements Runnable {
		private static final long ONE_MINUTE_OF_MILLISECONDS = 60000L;

		private volatile boolean inProgress;

		/**
		 * Initializes a new instance of {@code AppspaceCloudDataLoader}.
		 */
		public AppspaceCloudDataLoader() {
			this.inProgress = true;
		}

		/**
		 * Continuously runs data collection while the process is active.
		 */
		@Override
		public void run() {
			while (this.inProgress) {
				long startCycle = System.currentTimeMillis();
				Util.delayExecution(500);
				if (!this.inProgress) {
					logger.debug("Main data collection thread is not in progress, breaking.");
					break;
				}
				updateAggregatorStatus();
				if (devicePaused) {
					logger.debug("The device communicator is paused, data collector is not active.");
					continue;
				}

				long currentTimestamp = System.currentTimeMillis();
				if (!flag && nextCollectionTime < currentTimestamp) {
					Map<String, List<Property>> newDevicesProperties = new HashMap<>();
					devices.forEach(device -> {
						List<Property> properties = getDevicePropertiesDataByDeviceId(device.getId());
						newDevicesProperties.put(device.getId(), properties);
					});
					cachedDevicesProperties.clear();
					cachedDevicesProperties.putAll(newDevicesProperties);
					flag = true;
				}

				if (!this.inProgress) {
					logger.debug("Main data collection thread is not in progress, breaking.");
					break;
				}
				while (nextCollectionTime > System.currentTimeMillis()) {
					Util.delayExecution(1000);
				}
				if (flag) {
					nextCollectionTime = System.currentTimeMillis() + ONE_MINUTE_OF_MILLISECONDS;
					lastMonitoringCycleDuration = System.currentTimeMillis() - startCycle;
					flag = false;
				}
			}
		}

		/**
		 * Stops the data collection process.
		 */
		public void stop() {
			this.inProgress = false;
		}
	}

	/**
	 * Number of threads in the executor service thread pool.
	 */
	private static final int THREAD_POOL_NUMBER = 1;

	/**
	 * Timeout duration (in milliseconds) for retrieving statistics.
	 */
	private static final long RETRIEVE_STATISTICS_TIMEOUT = 3 * 60 * 1000L;

	/**
	 * Lock used for thread synchronization to ensure safe concurrent access.
	 */
	private final ReentrantLock reentrantLock;

	/**
	 * Holds the application configuration properties loaded from the {@code application.properties} file.
	 */
	private final Properties applicationProperties;

	/**
	 * Object mapper for JSON serialization and deserialization.
	 */
	private final ObjectMapper objectMapper;

	/**
	 * Stores locally extended statistics for devices.
	 */
	private final ExtendedStatistics localExtendedStatistics;

	/**
	 * Thread pool for executing background tasks.
	 */
	private ExecutorService executorService;

	/**
	 * Data loader responsible for retrieving data from Appspace Cloud.
	 */
	private AppspaceCloudDataLoader appspaceCloudDataLoader;

	/**
	 * Timestamp indicating the next scheduled data collection time.
	 */
	private long nextCollectionTime;

	/**
	 * Duration (in milliseconds) of the last monitoring cycle.
	 */
	private Long lastMonitoringCycleDuration;

	/**
	 * Indicates whether the device is paused.
	 */
	private volatile boolean devicePaused;

	/**
	 * Timestamp indicating the last valid retrieval time for statistics.
	 */
	private volatile long validRetrieveStatisticsTimestamp;

	/**
	 * General-purpose flag used for internal logic control.
	 */
	private volatile boolean flag;

	/**
	 * Authorization instance used for handling access control.
	 */
	private Authorization authorization;

	/**
	 * List of devices being monitored or managed.
	 */
	private List<Device> devices;

	/**
	 * Map storing device properties, with device IDs as keys and property lists as values.
	 */
	private Map<String, List<Property>> devicesProperties;

	/**
	 * Cached map of device properties to reduce redundant computations.
	 */
	private final Map<String, List<Property>> cachedDevicesProperties;

	/**
	 * List of aggregated devices containing summarized data.
	 */
	private List<AggregatedDevice> aggregatedDevices;

	/**
	 * Timestamp indicating when the access token was last updated.
	 */
	private LocalDateTime accessTokenTime;

	/**
	 * Identifier for the device location.
	 */
	private String locationId;

	/**
	 * Initializes an instance of {@code AppspaceCloudCommunicator}.
	 */
	public AppspaceCloudCommunicator() {
		this.reentrantLock = new ReentrantLock();
		this.localExtendedStatistics = new ExtendedStatistics();
		this.objectMapper = new ObjectMapper();
		this.applicationProperties = new Properties();

		this.executorService = null;
		this.appspaceCloudDataLoader = null;
		this.nextCollectionTime = System.currentTimeMillis();
		this.lastMonitoringCycleDuration = null;
		this.devicePaused = true;
		this.flag = false;
		this.authorization = new Authorization();
		this.devices = new ArrayList<>();
		this.aggregatedDevices = new ArrayList<>();
		this.devicesProperties = new HashMap<>();
		this.cachedDevicesProperties = new HashMap<>();
		this.accessTokenTime = LocalDateTime.now();

		this.loadProperties(this.applicationProperties);
		this.setAuthenticationScheme(AuthenticationScheme.None);
		this.setTrustAllCertificates(true);
	}

	/**
	 * Retrieves {@link #locationId}
	 *
	 * @return value of {@link #locationId}
	 */
	public String getLocationId() {
		return this.locationId;
	}

	/**
	 * Sets {@link #locationId} value
	 *
	 * @param locationId new value of {@link #locationId}
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	@Override
	protected void authenticate() throws Exception {
	}

	@Override
	protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) throws Exception {
		if (this.authorization != null && StringUtils.isNotNullOrEmpty(this.authorization.getAccessToken())) {
			headers.setBearerAuth(this.authorization.getAccessToken());
		}
		return headers;
	}

	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		this.reentrantLock.lock();
		try {
			this.setupData();
			Map<String, String> properties = new LinkedHashMap<>();
			Arrays.stream(AggregatorProperty.values()).forEach(property -> {
				String value;
				if (AggregatorProperty.LAST_MONITORING_CYCLE_DURATION.getName().equals(property.getName())) {
					value = Util.getAggregatorProperty(property, this.lastMonitoringCycleDuration);
				} else if (AggregatorProperty.MONITORED_DEVICES_TOTAL.getName().equals(property.getName())) {
					value = Util.getAggregatorProperty(property, this.aggregatedDevices.size());
				} else {
					value = Util.getAggregatorProperty(property, this.applicationProperties);
				}

				if (value != null) {
					properties.put(property.getName(), value);
				}
			});
			this.localExtendedStatistics.setStatistics(properties);
		} finally {
			this.reentrantLock.unlock();
		}
		return Collections.singletonList(this.localExtendedStatistics);
	}

	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
		this.setupAggregatedData();
		if (this.devices.isEmpty()) {
			this.logger.info("Device list is empty");
			return Collections.emptyList();
		}
		if (this.devicesProperties.isEmpty()) {
			this.logger.info("Devices properties map is empty");
			return Collections.emptyList();
		}

		List<AggregatedDevice> newAggregatedDevices = new ArrayList<>();
		this.devicesProperties.entrySet().parallelStream().forEach(deviceProperties -> {
			Device device = this.devices.stream().filter(d -> d.getId().equals(deviceProperties.getKey())).findAny().orElse(null);
			if (device != null) {
				AggregatedDevice aggregatedDevice = new AggregatedDevice();
				aggregatedDevice.setDeviceId(deviceProperties.getKey());
				aggregatedDevice.setDeviceName(device.getName());
				aggregatedDevice.setDeviceOnline(device.getStatus().equals(Constant.ONLINE));
				if (device.getSerialNumber() != null) {
					aggregatedDevice.setSerialNumber(device.getSerialNumber());
				}
				if (device.getMacAddress() != null) {
					aggregatedDevice.setMacAddresses(Collections.singletonList(device.getMacAddress()));
				}
				aggregatedDevice.setProperties(this.generatePropertiesForAggregatedDevice(device, deviceProperties.getValue()));

				newAggregatedDevices.add(aggregatedDevice);
			}
		});
		this.aggregatedDevices = newAggregatedDevices;

		return this.aggregatedDevices;
	}

	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics(List<String> list) throws Exception {
		return this.retrieveMultipleStatistics().stream()
				.filter(aggregatedDevice -> list.contains(aggregatedDevice.getDeviceId()))
				.collect(Collectors.toList());
	}

	@Override
	protected void internalDestroy() {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal destroy is called.");
		}
		if (this.appspaceCloudDataLoader != null) {
			this.appspaceCloudDataLoader.stop();
			this.appspaceCloudDataLoader = null;
		}
		if (this.executorService != null) {
			this.executorService.shutdownNow();
			this.executorService = null;
		}
		if (this.localExtendedStatistics.getStatistics() != null) {
			this.localExtendedStatistics.getStatistics().clear();
		}
		this.nextCollectionTime = 0;
		this.aggregatedDevices.clear();
		this.cachedDevicesProperties.clear();
		this.devicesProperties.clear();
		this.devices.clear();
		this.authorization = new Authorization();
		super.internalDestroy();
	}

	/**
	 * Loads properties from the {@code application.properties} file into the provided {@link Properties} object.
	 *
	 * @param properties The {@link Properties} object to load the configuration into.
	 * @throws ResourceNotReachableException if the properties file cannot be loaded.
	 */
	private void loadProperties(Properties properties) {
		try {
			properties.load(getClass().getResourceAsStream("/application.properties"));
		} catch (Exception e) {
			throw new ResourceNotReachableException(Constant.UNABLE_TO_READ_PROPERTIES_FILE);
		}
	}

	/**
	 * Sets up the necessary data for processing.
	 */
	private void setupData() {
		this.authorization = this.getAuthorizationData();
		this.devices = this.getDevicesData();
	}

	/**
	 * Sets up aggregated data by retrieving device properties.
	 */
	private void setupAggregatedData() {
		this.devicesProperties = this.getDevicesPropertiesData();
	}

	/**
	 * Retrieves properties data for all devices.
	 * If the device list is empty, it returns the cached device properties.
	 * If the executor service is not initialized, it creates a new thread pool
	 * and starts the {@link AppspaceCloudDataLoader} to periodically fetch data.
	 *
	 * @return a map containing device IDs as keys and their corresponding properties as values.
	 */
	private Map<String, List<Property>> getDevicesPropertiesData() {
		if (this.devices.isEmpty()) {
			this.logger.info("Device list is empty");
			return this.cachedDevicesProperties;
		}

		if (this.executorService == null) {
			this.executorService = Executors.newFixedThreadPool(THREAD_POOL_NUMBER);
			this.appspaceCloudDataLoader = new AppspaceCloudDataLoader();

			this.executorService.submit(this.appspaceCloudDataLoader);
		}
		this.nextCollectionTime = System.currentTimeMillis();
		this.updateValidRetrieveStatisticsTimestamp();

		return this.cachedDevicesProperties;
	}

	/**
	 * Retrieves authorization data from the API.
	 *
	 * @return an {@code Authorization} object.
	 */
	private Authorization getAuthorizationData() {
		try {
			if (this.authorization != null && Util.isNotTokenExpires(this.accessTokenTime)) {
				return this.authorization;
			}

			if (StringUtils.isNullOrEmpty(getLogin()) || StringUtils.isNullOrEmpty(getPassword())) {
				throw new NotAuthorizedException(Constant.AUTHORIZATION_API_FAILED);
			}
			AuthorizationReq req = new AuthorizationReq(getPassword(), getLogin());
			Authorization authorizationRes = doPost(Endpoint.AUTHORIZATION_TOKEN, req, Authorization.class);
			this.accessTokenTime = LocalDateTime.now().plusSeconds(authorizationRes.getExpiresIn());

			return authorizationRes;
		} catch (Exception e) {
			throw new NotAuthorizedException(Constant.AUTHORIZATION_API_FAILED, e);
		}
	}

	/**
	 * Fetches a list of devices from the API.
	 *
	 * @return List of {@link Device}.
	 */
	private List<Device> getDevicesData() {
		try {
			String url = Endpoint.DEVICES.replace(Endpoint.LOCATION_ID, StringUtils.isNullOrEmpty(this.locationId) ? "" : this.locationId);
			String response = doGet(url);

			return this.objectMapper.readValue(
					this.objectMapper.readTree(response).get(Constant.ITEMS).toPrettyString(),
					new TypeReference<List<Device>>() {
					}
			);
		} catch (Exception e) {
			throw new ResourceNotReachableException(Constant.DEVICES_API_FAILED, e);
		}
	}

	/**
	 * Retrieves device properties for a given device ID.
	 *
	 * @param deviceId The ID of the device.
	 * @return A list of {@link Property}.
	 */
	public List<Property> getDevicePropertiesDataByDeviceId(String deviceId) {
		try {
			String url = Endpoint.DEVICE_PROPERTIES.replace(Endpoint.DEVICE_ID, deviceId);
			String response = doGet(url);

			return this.objectMapper.readValue(
					this.objectMapper.readTree(response).get(Constant.ITEMS).toPrettyString(),
					new TypeReference<List<Property>>() {
					}
			);
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}


	/**
	 * Generates properties for an aggregated device.
	 *
	 * @param device    The device object.
	 * @param properties List of properties for the device.
	 * @return A map of properties.
	 */
	private Map<String, String> generatePropertiesForAggregatedDevice(Device device, List<Property> properties) {
		Map<String, String> deviceProperties = new LinkedHashMap<>();

		Arrays.stream(GeneralProperty.values()).forEach(generalProperty -> {
			String value = Util.getDeviceValueByGeneralProperty(device, generalProperty);
			if (value != null) {
				deviceProperties.put(generalProperty.getName(), value);
			}
		});
		if (!properties.isEmpty()) {
			Arrays.stream(SettingProperty.values()).forEach(settingProperty -> {
				String value = Util.getDevicePropertyValueBySettingProperty(properties, settingProperty);
				if (value != null) {
					String name = Constant.SETTING_GROUP + "#" + settingProperty.getName();
					deviceProperties.put(name, value);
				}
			});
		}

		return deviceProperties;
	}

	/**
	 * Updates the aggregator status based on the current timestamp.
	 */
	private synchronized void updateAggregatorStatus() {
		if (validRetrieveStatisticsTimestamp > 0L) {
			devicePaused = validRetrieveStatisticsTimestamp < System.currentTimeMillis();
		} else {
			devicePaused = false;
		}
	}

	/**
	 * Updates the {@code validRetrieveStatisticsTimestamp}.
	 */
	private synchronized void updateValidRetrieveStatisticsTimestamp() {
		validRetrieveStatisticsTimestamp = System.currentTimeMillis() + RETRIEVE_STATISTICS_TIMEOUT;
		updateAggregatorStatus();
	}
}
