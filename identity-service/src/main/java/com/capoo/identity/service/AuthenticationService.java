package com.capoo.identity.service;

import java.text.ParseException;

import com.capoo.identity.dto.request.AuthenticationRequest;
import com.capoo.identity.dto.request.IntrospectRequest;
import com.capoo.identity.dto.request.LogoutRequest;
import com.capoo.identity.dto.request.RefreshRequest;
import com.capoo.identity.dto.response.AuthenticationResponse;
import com.capoo.identity.dto.response.IntrospectResponse;
import com.nimbusds.jose.JOSEException;

public interface AuthenticationService {
    IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException;

    String authenticate(AuthenticationRequest request);

    AuthenticationResponse oundboundAuthenticate(String code);

    void logout(LogoutRequest request) throws ParseException, JOSEException;

    AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException;

    void cleanupExpiredTokens();
}
