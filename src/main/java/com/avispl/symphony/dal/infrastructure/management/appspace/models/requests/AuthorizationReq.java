/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.appspace.models.requests;

/**
 * AuthorizationReq class
 * <br>
 * Represents a request object for authorization.
 * This class is used to request a new authorization token using a refresh token.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 3/20/2025
 * @since 1.0.0
 */
public class AuthorizationReq {
	private final String context;
	private final String grantType;
	private final String refreshToken;
	private final String subjectId;
	private final String subjectType;

	/**
	 * Constructs an {@code AuthorizationReq} with the specified refresh token and subject ID.
	 * The grant type is set to "refreshToken" and the subject type is "Application".
	 *
	 * @param refreshToken the refresh token used for authorization
	 * @param subjectId    the subject ID associated with the authorization request
	 */
	public AuthorizationReq(String refreshToken, String subjectId) {
		this.context = null;
		this.grantType = "refreshToken";
		this.refreshToken = refreshToken;
		this.subjectId = subjectId;
		this.subjectType = "Application";
	}

	/**
	 * Retrieves {@link #context}
	 *
	 * @return value of {@link #context}
	 */
	public String getContext() {
		return context;
	}

	/**
	 * Retrieves {@link #grantType}
	 *
	 * @return value of {@link #grantType}
	 */
	public String getGrantType() {
		return grantType;
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
	 * Retrieves {@link #subjectId}
	 *
	 * @return value of {@link #subjectId}
	 */
	public String getSubjectId() {
		return subjectId;
	}

	/**
	 * Retrieves {@link #subjectType}
	 *
	 * @return value of {@link #subjectType}
	 */
	public String getSubjectType() {
		return subjectType;
	}
}
