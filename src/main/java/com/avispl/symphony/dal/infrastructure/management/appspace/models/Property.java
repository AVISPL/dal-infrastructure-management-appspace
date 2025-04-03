/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Property class
 * <br>
 * Represents a configurable property with the various properties.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Property {
	private boolean disabled;
	private String key;
	private Origin origin;
	private String value;

	/**
	 * The constructor for JSON mapping.
	 */
	public Property() {
		//	Using for mapping from json response to Property object
	}

	/**
	 * Retrieves {@link #disabled}
	 *
	 * @return value of {@link #disabled}
	 */
	public boolean getDisabled() {
		return disabled;
	}

	/**
	 * Retrieves {@link #key}
	 *
	 * @return value of {@link #key}
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Retrieves {@link #origin}
	 *
	 * @return value of {@link #origin}
	 */
	public Origin getOrigin() {
		return origin;
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
