/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregated;

/**
 * SettingProperty enum
 * <br>
 * Enum representing setting properties of an aggregated device.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
public enum SettingProperty {
	CONTENT_DOWNLOAD_MAX_SPEED("ContentDownloadMaxSpeed", "CDNAgent.ContentDownloadMaxSpeed"),
	CONTENT_DOWNLOAD_CYCLE("ContentDownloadCycle", "CDNAgent.ContentDownloadCycle"),
	REBOOT_EVENTS("RebootEvents", "System.RebootEvents"),
	SCREEN_BURN_MODE("ScreenBurnMode", "Display.Screen Burn Mode"),
	SCREEN_BURN_TIME("ScreenBurnTime", "Display.Screen Burn Time"),
	CONTENT_MAX_CONCURRENT_DOWNLOADS("ContentMaxConcurrentDownloads", "CDNAgent.ContentMaxConcurrentDownloads"),
	RESPONSIVE("Responsive", "Display.Responsive"),
	PUBLIC_APP_SIZE("PublicAppSize", "application.publicapp.size"),
	PLAYER_HAS_CONSTELLATION_TOKEN("PlayerHasConstellationToken", "player.hasConstellationToken");

	private final String name;
	private final String key;

	/**
	 * Constructs a {@code SettingProperty} with the given properties.
	 *
	 * @param name the display name of the setting
	 * @param key the internal key representing the setting
	 */
	SettingProperty(String name, String key) {
		this.name = name;
		this.key = key;
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
	 * Retrieves {@link #key}
	 *
	 * @return value of {@link #key}
	 */
	public String getKey() {
		return key;
	}
}