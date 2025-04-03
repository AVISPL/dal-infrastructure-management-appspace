/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * License class
 * <br>
 * Represents a license associated with a device.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class License {
	private String licenseTypeId;
	private List<Source> sources;
	private String name;

	/**
	 * The constructor for JSON mapping.
	 */
	public License() {
		//	Using for mapping from json response to License object
	}

	/**
	 * Retrieves {@link #licenseTypeId}
	 *
	 * @return value of {@link #licenseTypeId}
	 */
	public String getLicenseTypeId() {
		return licenseTypeId;
	}

	/**
	 * Retrieves {@link #sources}
	 *
	 * @return value of {@link #sources}
	 */
	public List<Source> getSources() {
		return sources;
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
