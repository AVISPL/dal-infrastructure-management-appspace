/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.security.auth.login.FailedLoginException;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.error.CommandFailureException;
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
		private static final long POLLING_CYCLE_INTERVAL = 60000L;

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
				long startCycle = System.currentTimeMillis();
				if (!flag && nextCollectionTime < System.currentTimeMillis()) {
					if (deviceMetadataRetrievalInterval <= 0 || startCycle >= nextDeviceMetadataRetrievalTime) {
						try {
							devices = getDevicesData();
						} catch (Exception e) {
							logger.error("Error occurred during devices list retrieval.", e);
						}
						if (deviceMetadataRetrievalInterval > 0) {
							nextDeviceMetadataRetrievalTime = startCycle + deviceMetadataRetrievalInterval;
						}
					}
					if (devices.isEmpty()) {
						logger.info("Device list is empty");
					} else {
						Map<String, List<Property>> newDevicesProperties = new ConcurrentHashMap<>();
						List<Callable<Void>> deviceFetchTasks = new ArrayList<>();
						for (Device device : devices) {
							if (!this.inProgress) {
								break;
							}
							deviceFetchTasks.add(() -> {
								List<Property> properties = getDevicePropertiesDataByDeviceId(device.getId());
								newDevicesProperties.put(device.getId(), properties);
								return null;
							});
						}
						try {
							deviceCollectionExecutorService.invokeAll(deviceFetchTasks);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							logger.warn("Interrupted while waiting for device properties retrieval to complete.", e);
						}
						cachedDevicesProperties = Collections.unmodifiableMap(newDevicesProperties);
					}
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
					try {
						nextCollectionTime = System.currentTimeMillis() + (getMonitoringRate() * POLLING_CYCLE_INTERVAL);
					} catch (NoSuchMethodError nsme) {
						nextCollectionTime = System.currentTimeMillis() + POLLING_CYCLE_INTERVAL;
						logger.warn("Unsupported feature: getMonitoringRate isn't available on current Cloud Connector version.", nsme);
					}
					lastMonitoringCycleDuration = Math.max((System.currentTimeMillis() - startCycle) / 1000, 1L);
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
	private static final long RETRIEVE_STATISTICS_TIMEOUT = 10 * 60 * 1000L;

	/**
	 * Number of attempts made to retrieve an authorization token before giving up,
	 * used to smooth over transient (e.g. 5xx) failures from the authorization endpoint.
	 */
	private static final int AUTHORIZATION_RETRY_ATTEMPTS = 3;

	/**
	 * Delay, in milliseconds, between authorization retry attempts.
	 */
	private static final long AUTHORIZATION_RETRY_DELAY = 1000L;

	/**
	 * Lock used for thread synchronization to ensure safe concurrent access.
	 */
	private final ReentrantLock reentrantLock;

	/**
	 * Holds the application configuration properties loaded from the {@code application.properties} file.
	 */
	private final Properties applicationProperties;

	/** Device adapter instantiation timestamp. */
	private final long adapterInitializationTimestamp;

	/**
	 * Object mapper for JSON serialization and deserialization.
	 */
	private final ObjectMapper objectMapper;

	/**
	 * Stores locally extended statistics for devices.
	 */
	private final ExtendedStatistics localExtendedStatistics;

	/**
	 * Thread pool hosting the {@link AppspaceCloudDataLoader} background task.
	 */
	private ExecutorService executorService;

	/**
	 * Thread pool used to concurrently retrieve properties for individual devices.
	 */
	private ExecutorService deviceCollectionExecutorService;

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
	 * Reassigned wholesale by the background data loader thread and read from the caller thread,
	 * so it is kept volatile to make each newly fetched list visible without extra locking.
	 */
	private volatile List<Device> devices;

	/**
	 * Map storing device properties, with device IDs as keys and property lists as values.
	 */
	private Map<String, List<Property>> devicesProperties;

	/**
	 * Cached map of device properties to reduce redundant computations.
	 */
	private volatile Map<String, List<Property>> cachedDevicesProperties;

	/**
	 * List of aggregated devices containing summarized data.
	 */
	private List<AggregatedDevice> aggregatedDevices;

	/**
	 * Identifier for the device location.
	 */
	private String locationId;

	/**
	 * Number of devices requested per page when paginating the devices list endpoint.
	 */
	private int devicePageSize = 100;

	/**
	 * Number of threads used to concurrently retrieve properties for individual devices.
	 */
	private int deviceCollectionThreadCount = 8;

	/**
	 * Interval, in milliseconds, between successive device list (metadata) retrievals.
	 * Device metadata is relatively static compared to per-device properties, so it does not need to
	 * be re-fetched on every monitoring cycle. A value of {@code 0} or less disables the interval,
	 * forcing a fetch on every cycle.
	 */
	private long deviceMetadataRetrievalInterval = 5 * 60 * 1000L;

	/**
	 * Timestamp indicating when the device list is next allowed to be refreshed.
	 * Left at {@code 0} until the first fetch so that the first cycle always fetches.
	 */
	private volatile long nextDeviceMetadataRetrievalTime;

	/**
	 * Initializes an instance of {@code AppspaceCloudCommunicator}.
	 */
	public AppspaceCloudCommunicator() {
		this.reentrantLock = new ReentrantLock();
		this.localExtendedStatistics = new ExtendedStatistics();
		this.objectMapper = new ObjectMapper();
		this.applicationProperties = new Properties();
		this.adapterInitializationTimestamp = System.currentTimeMillis();

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

	/**
	 * Retrieves {@link #devicePageSize}
	 *
	 * @return value of {@link #devicePageSize}
	 */
	public int getDevicePageSize() {
		return this.devicePageSize;
	}

	/**
	 * Sets {@link #devicePageSize} value
	 *
	 * @param devicePageSize new value of {@link #devicePageSize}
	 */
	public void setDevicePageSize(int devicePageSize) {
		this.devicePageSize = devicePageSize;
	}

	/**
	 * Retrieves {@link #deviceCollectionThreadCount}
	 *
	 * @return value of {@link #deviceCollectionThreadCount}
	 */
	public int getDeviceCollectionThreadCount() {
		return this.deviceCollectionThreadCount;
	}

	/**
	 * Sets {@link #deviceCollectionThreadCount} value
	 *
	 * @param deviceCollectionThreadCount new value of {@link #deviceCollectionThreadCount}
	 */
	public void setDeviceCollectionThreadCount(int deviceCollectionThreadCount) {
		this.deviceCollectionThreadCount = deviceCollectionThreadCount;
	}

	/**
	 * Retrieves {@link #deviceMetadataRetrievalInterval}
	 *
	 * @return value of {@link #deviceMetadataRetrievalInterval}
	 */
	public long getDeviceMetadataRetrievalInterval() {
		return this.deviceMetadataRetrievalInterval;
	}

	/**
	 * Sets {@link #deviceMetadataRetrievalInterval} value
	 *
	 * @param deviceMetadataRetrievalInterval new value of {@link #deviceMetadataRetrievalInterval}
	 */
	public void setDeviceMetadataRetrievalInterval(long deviceMetadataRetrievalInterval) {
		this.deviceMetadataRetrievalInterval = deviceMetadataRetrievalInterval;
	}

	@Override
	protected void authenticate() throws Exception {
		if (StringUtils.isNullOrEmpty(this.getLogin()) || StringUtils.isNullOrEmpty(this.getPassword())) {
			throw new FailedLoginException(Constant.LOGIN_FAILED);
		}
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
			Map<String, String> dynamicProperties = new LinkedHashMap<>();
			Arrays.stream(AggregatorProperty.values()).forEach(property -> {
				String value;
				if (AggregatorProperty.LAST_MONITORING_CYCLE_DURATION.getName().equals(property.getName())) {
					value = Util.getAggregatorProperty(property, this.lastMonitoringCycleDuration);
					dynamicProperties.put(property.getName(), value);
				} else if (AggregatorProperty.MONITORED_DEVICES_TOTAL.getName().equals(property.getName())) {
					value = Util.getAggregatorProperty(property, this.aggregatedDevices.size());
					dynamicProperties.put(property.getName(), value);
				} else {
					try {
						if (AggregatorProperty.MONITORED_CYCLE_INTERVAL.getName().equals(property.getName())) {
							value = Util.getAggregatorProperty(property, this.getMonitoringRate());
						} else {
							value = Util.getAggregatorProperty(property, this.applicationProperties);
						}
						properties.put(property.getName(), value);
					} catch (NoSuchMethodError nsme) {
						logger.warn("Unsupported feature: getMonitoringRate isn't available on current Cloud Connector version.", nsme);
					}
				}
			});
			this.localExtendedStatistics.setStatistics(properties);
			this.localExtendedStatistics.setDynamicStatistics(dynamicProperties);
		} finally {
			this.reentrantLock.unlock();
		}
		return Collections.singletonList(this.localExtendedStatistics);
	}

	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
		this.setupAggregatedData();
		List<Device> currentDevices = this.devices;
		if (currentDevices.isEmpty()) {
			this.logger.info("Device list is empty");
			return Collections.emptyList();
		}
		if (this.devicesProperties.isEmpty()) {
			this.logger.info("Devices properties map is empty");
			return Collections.emptyList();
		}

		List<AggregatedDevice> newAggregatedDevices = new ArrayList<>();
		this.devicesProperties.forEach((key, value) -> {
            Device device = currentDevices.stream().filter(d -> d.getId().equals(key)).findAny().orElse(null);
            if (device != null) {
                AggregatedDevice aggregatedDevice = new AggregatedDevice();
                aggregatedDevice.setDeviceId(key);
                aggregatedDevice.setDeviceName(device.getName());
                aggregatedDevice.setDeviceOnline(Constant.ONLINE.equals(device.getStatus()));
                if (device.getSerialNumber() != null) {
                    aggregatedDevice.setSerialNumber(device.getSerialNumber());
                }
                if (device.getMacAddress() != null) {
                    aggregatedDevice.setMacAddresses(Collections.singletonList(device.getMacAddress()));
                }
                aggregatedDevice.setProperties(this.generatePropertiesForAggregatedDevice(device, value));

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
		if (this.deviceCollectionExecutorService != null) {
			this.deviceCollectionExecutorService.shutdownNow();
			this.deviceCollectionExecutorService = null;
		}
		if (this.localExtendedStatistics.getStatistics() != null) {
			this.localExtendedStatistics.getStatistics().clear();
		}
		this.nextCollectionTime = 0;
		this.aggregatedDevices.clear();
		this.cachedDevicesProperties = Collections.emptyMap();
		this.devicesProperties = Collections.emptyMap();
		this.devices.clear();
		this.authorization = null;
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
			properties.setProperty(AggregatorProperty.ADAPTER_UPTIME.getProperty(), String.valueOf(this.adapterInitializationTimestamp));
			properties.setProperty(AggregatorProperty.MONITORED_DEVICES_TOTAL.getProperty(), Constant.NOT_AVAILABLE);
			properties.setProperty(AggregatorProperty.LAST_MONITORING_CYCLE_DURATION.getProperty(), Constant.NOT_AVAILABLE);
			try {
				properties.setProperty(AggregatorProperty.MONITORED_CYCLE_INTERVAL.getProperty(), String.valueOf(this.getMonitoringRate()));
			} catch (NoSuchMethodError nsme) {
				logger.warn("Unsupported feature: getMonitoringRate isn't available on current Cloud Connector version.", nsme);
			}
		} catch (Exception e) {
			this.logger.error(Constant.UNABLE_TO_READ_PROPERTIES_FILE, e);
		}
	}

	/**
	 * Sets up the necessary data for processing.
	 */
	private void setupData() throws FailedLoginException {
		this.authorization = this.getAuthorizationData();
	}

	/**
	 * Sets up aggregated data by retrieving device properties.
	 */
	private void setupAggregatedData() {
		this.devicesProperties = this.getDevicesPropertiesData();
	}

	/**
	 * Retrieves properties data for all devices.
	 * If the executor services are not initialized, it creates the thread pools used to
	 * run the {@link AppspaceCloudDataLoader} and to concurrently fetch per-device properties,
	 * then starts the {@link AppspaceCloudDataLoader} to periodically fetch data.
	 * The device list itself is (re)populated by the {@link AppspaceCloudDataLoader}, so it may
	 * still be empty right after the very first call.
	 *
	 * @return a map containing device IDs as keys and their corresponding properties as values.
	 */
	private Map<String, List<Property>> getDevicesPropertiesData() {
		if (this.executorService == null) {
			this.executorService = Executors.newFixedThreadPool(THREAD_POOL_NUMBER);
			this.deviceCollectionExecutorService = Executors.newFixedThreadPool(resolveDeviceCollectionThreadCount());
			this.appspaceCloudDataLoader = new AppspaceCloudDataLoader();

			this.executorService.submit(this.appspaceCloudDataLoader);
		}
		this.nextCollectionTime = System.currentTimeMillis();
		this.updateValidRetrieveStatisticsTimestamp();

		return this.cachedDevicesProperties;
	}

	/**
	 * Resolves the actual thread count to use for {@link #deviceCollectionExecutorService}.
	 * All Appspace API calls target the same host, i.e. the same HTTP route, so sizing the pool
	 * above {@link #getMaxConnectionsPerRoute()} would only produce threads that block waiting for
	 * a pooled connection instead of adding real concurrency. A {@code maxConnectionsPerRoute} of
	 * {@code 0} or less means it has not been explicitly configured, in which case the underlying
	 * HTTP client falls back to its own default and {@link #deviceCollectionThreadCount} is used as-is.
	 *
	 * @return the number of threads to allocate to {@link #deviceCollectionExecutorService}.
	 */
	private int resolveDeviceCollectionThreadCount() {
		int maxConnectionsPerRoute = this.getMaxConnectionsPerRoute();
		int threadCount = maxConnectionsPerRoute > 0 ? Math.min(this.deviceCollectionThreadCount, maxConnectionsPerRoute) : this.deviceCollectionThreadCount;

		return Math.max(threadCount, 1);
	}

	/**
	 * Retrieves authorization data from the API.
	 * A {@code 401}/{@code 403} response means the credentials themselves are rejected and is
	 * surfaced immediately as a {@link FailedLoginException}. Any other failure (e.g. a transient
	 * {@code 5xx}) is retried up to {@link #AUTHORIZATION_RETRY_ATTEMPTS} times before giving up,
	 * since it does not indicate a credentials problem.
	 *
	 * @return an {@code Authorization} object.
	 */
	private Authorization getAuthorizationData() throws FailedLoginException {
		AuthorizationReq req = new AuthorizationReq(getPassword(), getLogin());
		Exception lastError = null;
		for (int attempt = 1; attempt <= AUTHORIZATION_RETRY_ATTEMPTS; attempt++) {
			try {
				return doPost(Endpoint.AUTHORIZATION_TOKEN, req, Authorization.class);
			} catch (FailedLoginException e) {
				throw e;
			} catch (CommandFailureException e) {
				int statusCode = e.getStatusCode();
				if (statusCode == HttpStatus.UNAUTHORIZED.value() || statusCode == HttpStatus.FORBIDDEN.value()) {
					FailedLoginException fle = new FailedLoginException(e.getMessage());
					fle.initCause(e);
					throw fle;
				}
				lastError = e;
				logger.warn(String.format("Authorization attempt %s/%s failed with status %s, retrying.", attempt, AUTHORIZATION_RETRY_ATTEMPTS, statusCode), e);
			} catch (Exception e) {
				throw new RuntimeException(Constant.AUTHORIZATION_API_FAILED, e);
			}
			if (attempt < AUTHORIZATION_RETRY_ATTEMPTS) {
				Util.delayExecution(AUTHORIZATION_RETRY_DELAY);
			}
		}
		throw new ResourceNotReachableException(Constant.AUTHORIZATION_API_FAILED, lastError);
	}

	/**
	 * Fetches the full list of devices from the API, paginating through the devices endpoint
	 * using {@link #devicePageSize} as the page size. The endpoint's {@code size} field does not
	 * reliably reflect the total device count (observed to stay fixed regardless of the requested
	 * {@code limit} or the actual total), so it is not used here. Instead, pagination stops as soon
	 * as a page returns fewer devices than requested, which is the standard end-of-data signal for
	 * offset/limit pagination.
	 *
	 * @return List of {@link Device}.
	 */
	private List<Device> getDevicesData() {
		List<Device> allDevices = new ArrayList<>();
		int limit = Math.max(this.devicePageSize, 1);
		int start = 0;
		try {
			while (true) {
				String url = String.format(Endpoint.DEVICES, start, limit, StringUtils.isNullOrEmpty(this.locationId) ? "" : this.locationId);
				String response = doGet(url);
				JsonNode root = this.objectMapper.readTree(response);

				List<Device> pageDevices = this.objectMapper.readValue(
						root.get(Constant.ITEMS).toPrettyString(),
						new TypeReference<List<Device>>() {
						}
				);
				allDevices.addAll(pageDevices);

				if (pageDevices.size() < limit) {
					break;
				}
				start += pageDevices.size();
			}
		} catch (Exception e) {
			throw new RuntimeException(String.format(Constant.FETCH_DATA_FAILED, Endpoint.DEVICES), e);
		}
		return allDevices;
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
