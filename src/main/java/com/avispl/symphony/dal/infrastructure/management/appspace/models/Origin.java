/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Origin class
 * <br>
 * Represents the origin of a device property or setting.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Origin {
	private String id;
	@JsonProperty("isOverridden")
	private boolean isOverridden;
	private String name;
	private String type;
	private String value;

	/**
	 * The constructor for JSON mapping.
	 */
	public Origin() {
		//	Using for mapping from json response to Origin object
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
	 * Retrieves {@link #isOverridden}
	 *
	 * @return value of {@link #isOverridden}
	 */
	public boolean isOverridden() {
		return isOverridden;
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
	 * Retrieves {@link #type}
	 *
	 * @return value of {@link #type}
	 */
	public String getType() {
		return type;
	}

	/**
	 * Retrieves {@link #value}
	 *
	 * @return value of {@link #value}
	 */
	public String getValue() {
		return value;
	}
}
