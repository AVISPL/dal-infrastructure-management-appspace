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
	ADAPTER_BUILD_DATE("AdapterBuildDate", "adapter.build.date"),
	ADAPTER_UPTIME("AdapterUptime", "adapter.uptime"),
	ADAPTER_UPTIME_MIN("AdapterUptime(min)", "adapter.uptime"),
	ADAPTER_VERSION("AdapterVersion", "adapter.version"),
	LAST_MONITORING_CYCLE_DURATION("LastMonitoringCycleDuration(sec)", "adapter.last.cycle.duration"),
	MONITORED_DEVICES_TOTAL("MonitoredDevicesTotal", "adapter.devices.total"),
	MONITORED_CYCLE_INTERVAL("MonitoringCycleInterval(min)", "adapter.cycle.interval")
	;

	private final String name;
	private final String property;

	/**
	 * Constructs a {@code AggregatorProperty} with the given properties.
	 *
	 * @param name the name of the aggregator property
	 */
	AggregatorProperty(String name, String property) {
		this.name = name;
		this.property = property;
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
	 * Retrieves {@link #property}
	 *
	 * @return value of {@link #property}
	 */
	public String getProperty() {
		return property;
	}
}
