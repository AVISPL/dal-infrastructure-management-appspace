/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Source class
 * <br>
 * Represents a source with the various properties.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Source {
	private String id;
	private String type;

	/**
	 * The constructor for JSON mapping.
	 */
	public Source() {
		//	Using for mapping from json response to Source object
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
	 * Retrieves {@link #type}
	 *
	 * @return value of {@link #type}
	 */
	public String getType() {
		return type;
	}
}
