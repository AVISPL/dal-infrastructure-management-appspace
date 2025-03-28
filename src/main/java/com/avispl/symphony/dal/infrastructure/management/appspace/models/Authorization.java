/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Authorization class
 * <br>
 * Represents an authorization response containing access and refresh tokens.
 * This class is used to map JSON responses from authentication services.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Authorization {
	private String accessToken;
	private String refreshToken;
	private String idToken;
	private String tokenType;
	private int expiresIn;
	private int refreshTokenExpiresIn;

	/**
	 * The constructor for JSON mapping.
	 */
	public Authorization() {
		// Used for mapping from JSON response to Authorization object
	}

	/**
	 * Retrieves {@link #accessToken}
	 *
	 * @return value of {@link #accessToken}
	 */
	public String getAccessToken() {
		return accessToken;
	}

	/**
	 * Sets {@link #accessToken} value
	 *
	 * @param accessToken new value of {@link #accessToken}
	 */
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	/**
	 * Retrieves {@link #refreshToken}
	 *
	 * @return value of {@link #refreshToken}
	 */
	public String getRefreshToken() {
		return refreshToken;
	}

	/**
	 * Sets {@link #refreshToken} value
	 *
	 * @param refreshToken new value of {@link #refreshToken}
	 */
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	/**
	 * Retrieves {@link #idToken}
	 *
	 * @return value of {@link #idToken}
	 */
	public String getIdToken() {
		return idToken;
	}

	/**
	 * Sets {@link #idToken} value
	 *
	 * @param idToken new value of {@link #idToken}
	 */
	public void setIdToken(String idToken) {
		this.idToken = idToken;
	}

	/**
	 * Retrieves {@link #tokenType}
	 *
	 * @return value of {@link #tokenType}
	 */
	public String getTokenType() {
		return tokenType;
	}

	/**
	 * Sets {@link #tokenType} value
	 *
	 * @param tokenType new value of {@link #tokenType}
	 */
	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	/**
	 * Retrieves {@link #expiresIn}
	 *
	 * @return value of {@link #expiresIn}
	 */
	public int getExpiresIn() {
		return expiresIn;
	}

	/**
	 * Sets {@link #expiresIn} value
	 *
	 * @param expiresIn new value of {@link #expiresIn}
	 */
	public void setExpiresIn(int expiresIn) {
		this.expiresIn = expiresIn;
	}

	/**
	 * Retrieves {@link #refreshTokenExpiresIn}
	 *
	 * @return value of {@link #refreshTokenExpiresIn}
	 */
	public int getRefreshTokenExpiresIn() {
		return refreshTokenExpiresIn;
	}

	/**
	 * Sets {@link #refreshTokenExpiresIn} value
	 *
	 * @param refreshTokenExpiresIn new value of {@link #refreshTokenExpiresIn}
	 */
	public void setRefreshTokenExpiresIn(int refreshTokenExpiresIn) {
		this.refreshTokenExpiresIn = refreshTokenExpiresIn;
	}
}
