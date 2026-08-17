/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.common.constants;

/**
 * Endpoint class
 * <br>
 * A utility class that defines API endpoint constants used in the application.
 * This class cannot be instantiated.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
public class Endpoint {
	/**
	 * Private constructor to prevent instantiation.
	 */
	private Endpoint() {
	}

	//	Params
	public static final String DEVICE_ID = "{deviceId}";
	public static final String LOCATION_ID = "{locationId}";

	//	Endpoints
	private static final String PREFIX_API = "api/v3";
	public static final String AUTHORIZATION_TOKEN = PREFIX_API + "/authorization/token";
	public static final String DEVICES = PREFIX_API + "/devices?start=%s&limit=%s&LocationId=%s";
	public static final String DEVICE_PROPERTIES = PREFIX_API + "/networks/devices/" + DEVICE_ID + "/properties";
}
