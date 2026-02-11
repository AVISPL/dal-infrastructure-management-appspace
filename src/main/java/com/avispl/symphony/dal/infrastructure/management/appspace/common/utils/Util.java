/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.common.utils;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.dal.infrastructure.management.appspace.common.constants.Constant;
import com.avispl.symphony.dal.infrastructure.management.appspace.models.Device;
import com.avispl.symphony.dal.infrastructure.management.appspace.models.License;
import com.avispl.symphony.dal.infrastructure.management.appspace.models.Property;
import com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregated.GeneralProperty;
import com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregated.SettingProperty;
import com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregator.AggregatorProperty;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * Util class
 * <br>
 *  A utility class providing helper methods for handling application properties,
 *  device attributes, and time-based calculations.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
public class Util {
	private static final Log LOG = LogFactory.getLog(Util.class);

	/**
	 * Private constructor to prevent instantiation.
	 */
	private Util() {
	}

	/**
	 * Retrieves a specific aggregator property value from the provided application properties.
	 *
	 * @param property               The {@link AggregatorProperty} enum value specifying which property to retrieve.
	 * @param applicationProperties  The {@link Properties} object containing application configuration values.
	 * @return The corresponding property value as a {@link String}, or {@code null} if the property is not recognized.
	 */
	public static String getAggregatorProperty(AggregatorProperty property, Properties applicationProperties) {
		if (applicationProperties == null) {
			LOG.warn(String.format("Skip adapter metadata mapping, the version properties data is null with %s", property));
			return null;
		}
		switch (property) {
			case ADAPTER_UPTIME: {
				return mapToUptime(applicationProperties.getProperty(property.getProperty()));
			}
			case ADAPTER_UPTIME_MIN: {
				return mapToUptimeMin(applicationProperties.getProperty(property.getProperty()));
			}
			default: {
				return applicationProperties.getProperty(property.getProperty());
			}
		}
	}

	/**
	 * Retrieves an aggregator property based on the specified type and integer value.
	 *
	 * @param property The type of aggregator property.
	 * @param value The number value associated with the property.
	 * @return The formatted property value.
	 */
	public static <T extends Number> String getAggregatorProperty(AggregatorProperty property, T value) {
		switch (property) {
			case LAST_MONITORING_CYCLE_DURATION:
				if (value == null) return "0";
				return String.valueOf(Math.round(value.longValue() / 1000.0));
			case MONITORED_DEVICES_TOTAL:
			case MONITORED_CYCLE_INTERVAL:
				return String.valueOf(value);
			default:
				return null;
		}
	}

	/**
	 * Retrieves a device property value based on the specified general property.
	 *
	 * @param device The device object.
	 * @param generalProperty The general property to retrieve.
	 * @return The value of the specified property.
	 * @throws ResourceNotReachableException if the device is null.
	 */
	public static String getDeviceValueByGeneralProperty(Device device, GeneralProperty generalProperty) {
		if (device == null) {
			throw new ResourceNotReachableException(Constant.DEVICE_NOT_NULL);
		}
		switch (generalProperty) {
			case DEVICE_TYPE:
				return device.getDeviceType();
			case LOCATION_NAME:
				return device.getLocationName();
			case GROUP_NAME:
				return device.getGroupName() == null || device.getGroupName().trim().isEmpty() ? null : device.getGroupName();
			case STATUS:
				return device.getStatus();
			case APP_VERSION:
				return device.getAppVersion();
			case LAST_ONLINE:
				return device.getLastOnlineAt();
			case TAGS:
				return device.getTags() == null || device.getTags().isEmpty() ? null : String.join(Constant.DELIMITER, device.getTags());
			case LICENSES:
				if (device.getLicenses() == null || device.getLicenses().isEmpty()) {
					return null;
				}
				List<String> licenseNames = device.getLicenses().stream().map(License::getName).collect(Collectors.toList());

				return String.join(Constant.DELIMITER, licenseNames);
			case CHANNEL_NAME:
				return device.getChannelName();
			case PLAYBACK_MODE:
				return device.getPlaybackMode();
			case PUBLISH_MODE:
				return device.getPublishMode();
			case AUTO_SYNC:
				return String.valueOf(device.getAutoSync());
			case SYNC_STATUS:
				return String.valueOf(device.getInSync());
			case IP_ADDRESS:
				return device.getIpAddress();
			case LOW_BANDWIDTH_MODE:
				return String.valueOf(device.getLowBandwidthMode());
			default:
				return null;
		}
	}

	/**
	 * Retrieves a device property value based on the specified setting property.
	 *
	 * @param properties The list of properties.
	 * @param settingProperty The setting property to retrieve.
	 * @return The value of the specified property.
	 * @throws ResourceNotReachableException if the properties list is empty.
	 */
	public static String getDevicePropertyValueBySettingProperty(List<Property> properties, SettingProperty settingProperty) {
		if (properties == null || properties.isEmpty()) {
			throw new ResourceNotReachableException(Constant.DEVICE_PROPERTIES_NOT_EMPTY);
		}
		if (settingProperty == null) {
			throw new ResourceNotReachableException(Constant.GENERAL_PROPERTY_NOT_NULL);
		}
		Property property = properties.stream().filter(p -> p.getKey().equals(settingProperty.getKey())).findFirst().orElse(null);
		if (property == null) return null;

		return property.getKey().equals(SettingProperty.RESPONSIVE.getKey())
				? property.getValue().toLowerCase()
				: property.getValue();
	}

	/**
	 * Delays execution for a specified duration.
	 *
	 * @param milliseconds The duration in milliseconds.
	 */
	public static void delayExecution(long milliseconds) {
		try {
			TimeUnit.MILLISECONDS.sleep(milliseconds);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Returns the elapsed uptime between the current system time and the given timestamp in milliseconds.
	 * <p>
	 * The input timestamp represents the start time in milliseconds (typically from {@link System#currentTimeMillis()}).
	 * The returned string represents the absolute duration in the format:
	 * "X day(s) Y hour(s) Z minute(s) W second(s)", omitting any zero-value units except seconds.
	 *
	 * @param uptime the start time in milliseconds as a string (e.g., "1717581000000")
	 * @return a formatted duration string like "2 d 3 hr 15 min 42 sec", or null if parsing fails
	 */
	private static String mapToUptime(String uptime) {
		try {
			if (StringUtils.isNullOrEmpty(uptime)) {
				LOG.warn("Skip uptime mapping, the value is null or empty");
				return null;
			}

			long uptimeSecond = (System.currentTimeMillis() - Long.parseLong(uptime)) / 1000;
			long seconds = uptimeSecond % 60;
			long minutes = uptimeSecond % 3600 / 60;
			long hours = uptimeSecond % 86400 / 3600;
			long days = uptimeSecond / 86400;
			StringBuilder rs = new StringBuilder();
			if (days > 0) {
				rs.append(days).append(" d ");
			}
			if (hours > 0) {
				rs.append(hours).append(" hr ");
			}
			if (minutes > 0) {
				rs.append(minutes).append(" min ");
			}
			rs.append(seconds).append(" sec");

			return rs.toString().trim();
		} catch (Exception e) {
			LOG.error("Failed to mapToUptime with uptime: " + uptime, e);
			return null;
		}
	}

	/**
	 * Returns the elapsed uptime in **whole minutes** between the current system time and the given timestamp in milliseconds.
	 * <p>
	 * The input timestamp represents the start time in milliseconds (typically from {@link System#currentTimeMillis()}).
	 * The returned string is the total number of minutes that have elapsed, excluding seconds.
	 *
	 * @param uptime the start time in milliseconds as a string (e.g., "1717581000000")
	 * @return a string representing the total number of elapsed minutes (e.g., "125"), or null if parsing fails
	 */
	private static String mapToUptimeMin(String uptime) {
		try {
			if (StringUtils.isNullOrEmpty(uptime)) {
				LOG.warn("Skip uptime min mapping, the value is null or empty");
				return null;
			}

			long uptimeSecond = (System.currentTimeMillis() - Long.parseLong(uptime)) / 1000;
			long minutes = uptimeSecond / 60;

			return String.valueOf(minutes);
		} catch (Exception e) {
			LOG.error("Failed to mapToUptimeMin with uptime: " + uptime, e);
			return null;
		}
	}
}
