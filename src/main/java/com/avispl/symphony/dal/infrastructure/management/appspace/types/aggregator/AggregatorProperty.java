/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.types.aggregator;

/**
 * GeneralProperty enum
 * <br>
 * Enum representing aggregator properties of an aggregator device.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
public enum AggregatorProperty {
	ADAPTER_BUILD_DATE("AdapterBuildDate"),
	ADAPTER_UPTIME("AdapterUptime"),
	ADAPTER_UPTIME_MIN("AdapterUptime(min)"),
	ADAPTER_VERSION("AdapterVersion"),
	LAST_MONITORING_CYCLE_DURATION("LastMonitoringCycleDuration(s)"),
	MONITORED_DEVICES_TOTAL("MonitoredDevicesTotal");

	private final String name;

	/**
	 * Constructs a {@code AggregatorProperty} with the given properties.
	 *
	 * @param name the name of the aggregator property
	 */
	AggregatorProperty(String name) {
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
