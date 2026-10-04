package com.capoo.profile.configuration;

import java.text.ParseException;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import com.nimbusds.jwt.SignedJWT;

@Component
public class CustomJwtDecoder implements JwtDecoder {

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
