/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Device class
 * <br>
 * Represents a device with various properties. This class is used to map device-related JSON data.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {
	private String id;
	private String deviceType;
	private String name;
	private String locationId;
	private String locationName;
	private String groupId;
	private String groupName;
	private String status;
	private String appVersion;
	private String lastOnlineAt;
	private List<String> tags;
	private List<License> licenses;
	private String channelId;
	private String channelName;
	private String playbackMode;
	private String publishMode;
	@JsonProperty("isAutoSync")
	private Boolean isAutoSync;
	@JsonProperty("isInSync")
	private Boolean isInSync;
	private String ipAddress;
	@JsonProperty("isLowBandwidthMode")
	private Boolean isLowBandwidthMode;
	private String macAddress;
	private String serialNumber;

	/**
	 * The constructor for JSON mapping.
	 */
	public Device() {
		//	Using for mapping from json response to Device object
	}

	/**
	 * Retrieves {@link #id}
	 *
	 * @return value of {@link #id}
	 */
	public String getId() {
		return id;
	}

	/**
	 * Retrieves {@link #deviceType}
	 *
	 * @return value of {@link #deviceType}
	 */
	public String getDeviceType() {
		return deviceType;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves {@link #locationId}
	 *
	 * @return value of {@link #locationId}
	 */
	public String getLocationId() {
		return locationId;
	}

	/**
	 * Retrieves {@link #locationName}
	 *
	 * @return value of {@link #locationName}
	 */
	public String getLocationName() {
		return locationName;
	}

	/**
	 * Retrieves {@link #groupId}
	 *
	 * @return value of {@link #groupId}
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Retrieves {@link #groupName}
	 *
	 * @return value of {@link #groupName}
	 */
	public String getGroupName() {
		return groupName;
	}

	/**
	 * Retrieves {@link #status}
	 *
	 * @return value of {@link #status}
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Retrieves {@link #appVersion}
	 *
	 * @return value of {@link #appVersion}
	 */
	public String getAppVersion() {
		return appVersion;
	}

	/**
	 * Retrieves {@link #lastOnlineAt}
	 *
	 * @return value of {@link #lastOnlineAt}
	 */
	public String getLastOnlineAt() {
		return lastOnlineAt;
	}

	/**
	 * Retrieves {@link #tags}
	 *
	 * @return value of {@link #tags}
	 */
	public List<String> getTags() {
		return tags;
	}

	/**
	 * Retrieves {@link #licenses}
	 *
	 * @return value of {@link #licenses}
	 */
	public List<License> getLicenses() {
		return licenses;
	}

	/**
	 * Retrieves {@link #channelId}
	 *
	 * @return value of {@link #channelId}
	 */
	public String getChannelId() {
		return channelId;
	}

	/**
	 * Retrieves {@link #channelName}
	 *
	 * @return value of {@link #channelName}
	 */
	public String getChannelName() {
		return channelName;
	}

	/**
	 * Retrieves {@link #playbackMode}
	 *
	 * @return value of {@link #playbackMode}
	 */
	public String getPlaybackMode() {
		return playbackMode;
	}

	/**
	 * Retrieves {@link #publishMode}
	 *
	 * @return value of {@link #publishMode}
	 */
	public String getPublishMode() {
		return publishMode;
	}

	/**
	 * Retrieves {@link #isAutoSync}
	 *
	 * @return value of {@link #isAutoSync}
	 */
	public Boolean getAutoSync() {
		return isAutoSync;
	}

	/**
	 * Retrieves {@link #isInSync}
	 *
	 * @return value of {@link #isInSync}
	 */
	public Boolean getInSync() {
		return isInSync;
	}

	/**
	 * Retrieves {@link #ipAddress}
	 *
	 * @return value of {@link #ipAddress}
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * Retrieves {@link #isLowBandwidthMode}
	 *
	 * @return value of {@link #isLowBandwidthMode}
	 */
	public Boolean getLowBandwidthMode() {
		return isLowBandwidthMode;
	}

	/**
	 * Retrieves {@link #macAddress}
	 *
	 * @return value of {@link #macAddress}
	 */
	public String getMacAddress() {
		return macAddress;
	}

	/**
	 * Retrieves {@link #serialNumber}
	 *
	 * @return value of {@link #serialNumber}
	 */
	public String getSerialNumber() {
		return serialNumber;
	}
}
