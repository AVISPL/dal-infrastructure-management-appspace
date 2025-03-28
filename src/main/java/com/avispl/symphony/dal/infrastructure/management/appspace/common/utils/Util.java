/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.common.utils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.dal.infrastructure.management.appspace.common.constants.Constant;
import com.avispl.symphony.dal.infrastructure.management.appspace.models.Device;
import com.avispl.symphony.dal.infrastructure.management.appspace.models.License;
import com.avispl.symphony.dal.infrastructure.management.appspace.models.Property;
import com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregated.GeneralProperty;
import com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregated.SettingProperty;
import com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregator.AggregatorProperty;

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
		String adapterBuildDate = applicationProperties.getProperty("adapter.build.date");

		switch (property) {
			case ADAPTER_BUILD_DATE: {
				return mapOffsetDatetimeToDatetime(adapterBuildDate);
			}
			case ADAPTER_UPTIME: {
				long elapsedMillis = Util.getElapsedMillis(adapterBuildDate);
				return formatElapsedTime(elapsedMillis);
			}
			case ADAPTER_UPTIME_MIN: {
				long elapsedMillis = Util.getElapsedMillis(adapterBuildDate);
				return String.valueOf(Util.getElapsedMinutes(elapsedMillis));
			}
			case ADAPTER_VERSION: {
				return applicationProperties.getProperty("adapter.version");
			}
			default: {
				return null;
			}
		}
	}

	/**
	 * Retrieves an aggregator property based on the specified type and integer value.
	 *
	 * @param property The type of aggregator property.
	 * @param value The integer value associated with the property.
	 * @return The formatted property value.
	 */
	public static String getAggregatorProperty(AggregatorProperty property, int value) {
		switch (property) {
			case LAST_MONITORING_CYCLE_DURATION:
				return String.valueOf(Math.max(value, 0));
			case MONITORED_DEVICES_TOTAL:
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
				return device.getGroupName();
			case STATUS:
				return device.getStatus();
			case APP_VERSION:
				return device.getAppVersion();
			case LAST_ONLINE:
				return device.getLastOnlineAt();
			case TAGS:
				return (device.getTags() == null || device.getTags().isEmpty()) ? null : String.join(Constant.DELIMITER, device.getTags());
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
	 * @throws ResourceNotReachableException if the settingProperty is null.
	 */
	public static String getDevicePropertyValueBySettingProperty(List<Property> properties, SettingProperty settingProperty) {
		if (properties == null || properties.isEmpty()) {
			throw new ResourceNotReachableException(Constant.DEVICE_PROPERTIES_NOT_EMPTY);
		}
		if (settingProperty == null) {
			throw new ResourceNotReachableException(Constant.GENERAL_PROPERTY_NOT_NULL);
		}
		Property property = properties.stream().filter(p -> p.getKey().equals(settingProperty.getKey())).findFirst().orElse(null);
		if (property == null) {
			throw new ResourceNotReachableException(Constant.PROPERTY_IS_NULL + " with {SettingProperty}: " + settingProperty);
		}

		return property.getKey().equals(SettingProperty.RESPONSIVE.getKey())
				? property.getValue().toLowerCase()
				: property.getValue();
	}

	/**
	 * Checks whether a given timestamp has not expired.
	 *
	 * @param checkDateTime The timestamp to check.
	 * @return {@code true} if the timestamp is in the future; otherwise, {@code false}.
	 */
	public static boolean isNotTokenExpires(LocalDateTime checkDateTime) {
		LocalDateTime currentDateTime = LocalDateTime.now();

		return checkDateTime.isAfter(currentDateTime);
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
	 * Converts an offset datetime string to a UTC datetime string.
	 *
	 * @param offsetDatetime The offset datetime string.
	 * @return The formatted UTC datetime string.
	 */
	private static String mapOffsetDatetimeToDatetime(String offsetDatetime) {
		return ZonedDateTime.parse(offsetDatetime).toInstant()
				.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(Constant.DATETIME_FORMAT));
	}

	/**
	 * Formats elapsed time from milliseconds into a human-readable format.
	 *
	 * @param totalMillis The total elapsed milliseconds.
	 * @return The formatted time string.
	 */
	private static String formatElapsedTime(long totalMillis) {
		long totalSeconds = totalMillis / 1000;
		long days = totalSeconds / (24 * 3600);
		long hours = (totalSeconds % (24 * 3600)) / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		return String.format("%d day(s) %d hour(s) %d minute(s) %d second(s)", days, hours, minutes, seconds);
	}

	/**
	 * Calculates the elapsed milliseconds from a given datetime string.
	 *
	 * @param dateTimeStr The datetime string.
	 * @return The elapsed time in milliseconds.
	 */
	private static long getElapsedMillis(String dateTimeStr) {
		Instant eventInstant = OffsetDateTime.parse(dateTimeStr).toInstant();
		Instant nowInstant = Instant.now(Clock.systemDefaultZone());

		return Duration.between(eventInstant, nowInstant).toMillis();
	}

	/**
	 * Converts elapsed milliseconds into minutes.
	 *
	 * @param totalMillis The total elapsed milliseconds.
	 * @return The equivalent time in minutes.
	 */
	private static long getElapsedMinutes(long totalMillis) {
		return totalMillis / (1000 * 60);
	}
}
