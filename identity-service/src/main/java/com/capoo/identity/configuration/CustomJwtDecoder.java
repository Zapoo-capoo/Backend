package com.capoo.identity.configuration;

import java.text.ParseException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.capoo.identity.service.AuthenticationService;
import com.nimbusds.jwt.SignedJWT;

@Component
public class CustomJwtDecoder implements JwtDecoder {
    @Value("${jwt.signerKey}")
    private String signerKey;

    private final AuthenticationService authenticationService;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    public CustomJwtDecoder(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            SignedJWT signed = SignedJWT.parse(token);
            return new Jwt(
                    token,
                    signed.getJWTClaimsSet().getIssueTime().toInstant(),
                    signed.getJWTClaimsSet().getExpirationTime().toInstant(),
                    signed.getHeader().toJSONObject(),
                    signed.getJWTClaimsSet().getClaims());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
