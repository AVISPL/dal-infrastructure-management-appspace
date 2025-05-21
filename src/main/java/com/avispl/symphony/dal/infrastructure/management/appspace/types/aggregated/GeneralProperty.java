/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregated;

/**
 * GeneralProperty enum
 * <br>
 * Enum representing general properties of an aggregated device.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
public enum GeneralProperty {
	DEVICE_TYPE("DeviceType"),
	LOCATION_NAME("LocationName"),
	GROUP_NAME("GroupName"),
	STATUS("Status"),
	APP_VERSION("AppVersion"),
	LAST_ONLINE("LastOnline(UTC)"),
	TAGS("Tags"),
	LICENSES("Licenses"),
	CHANNEL_NAME("ChannelName"),
	PLAYBACK_MODE("PlaybackMode"),
	PUBLISH_MODE("PublishMode"),
	AUTO_SYNC("AutoSync"),
	SYNC_STATUS("SyncStatus"),
	IP_ADDRESS("IPAddress"),
	LOW_BANDWIDTH_MODE("LowBandwidthMode");

	private final String name;

	/**
	 * Constructs a {@code GeneralProperty} with the given properties.
	 *
	 * @param name the name of the general property
	 */
	GeneralProperty(String name) {
		this.name = name;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}
}
