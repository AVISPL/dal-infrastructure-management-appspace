/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.common.constants;

/**
 * Constant class
 * <br>
 * A utility class that holds constant values used throughout the application.
 * This class cannot be instantiated.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
public class Constant {
	/**
	 * Private constructor to prevent instantiation.
	 */
	private Constant() {
	}

	public static final String ITEMS = "items";
	public static final String ONLINE = "Online";
	public static final String SETTING_GROUP = "Setting";
	public static final String DELIMITER = ", ";
	public static final String NOT_AVAILABLE = "N/A";

	//	Messages
	public static final String UNABLE_TO_READ_PROPERTIES_FILE = "Unable to load properties file: application.properties";
	public static final String LOGIN_FAILED = "Failed to login, please check the username and password";
	public static final String AUTHORIZATION_API_FAILED = "Failed to request to an authorization API";
	public static final String DEVICES_API_FAILED = "Failed to request to a devices API";
	public static final String DEVICE_NOT_NULL = "Device must not be null";
	public static final String DEVICE_PROPERTIES_NOT_EMPTY = "Device properties must not be empty";
	public static final String GENERAL_PROPERTY_NOT_NULL = "General property must not be null";
}
