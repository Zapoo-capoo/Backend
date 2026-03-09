package com.capoo.identity.service;

import com.capoo.identity.constant.PredefinedRole;
import com.capoo.identity.dto.request.*;
import com.capoo.identity.dto.response.AuthenticationResponse;
import com.capoo.identity.dto.response.IntrospectResponse;
import com.capoo.identity.entity.InvalidatedToken;
import com.capoo.identity.entity.Role;
import com.capoo.identity.entity.User;
import com.capoo.identity.exception.AppException;
import com.capoo.identity.exception.ErrorCode;
import com.capoo.identity.repository.InvalidatedTokenRepository;
import com.capoo.identity.repository.httpClient.google.OutBoundIdentityClient;
import com.capoo.identity.repository.UserRepository;
import com.capoo.identity.repository.httpClient.google.OutboundUserClient;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationService {
    final UserRepository userRepository;
    final InvalidatedTokenRepository invalidatedTokenRepository;
    final OutBoundIdentityClient outBoundIdentityClient;
    final OutboundUserClient outboundUserClient;
    @Value("${jwt.signerKey}")
    String signerKey;
    @Value("${jwt.valid-duration}")
    Long VALID_DURATION;
    @Value("${jwt.refreshable-duration}")
    Long REFRESHABLE_DURATION;
    @Value("${google.client.id}")
    String CLIENT_ID;
    @Value("${google.client.secret}")
    String CLIENT_SECRET;
    @Value("${google.redirectUri}")
    String REDIRECT_URI;
    String GRANT_TYPE="authorization_code";
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token,false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder().valid(isValid).build();
    }
    public String authenticate(AuthenticationRequest request) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        boolean authenticated=passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String token=generateToken(user);
        return token;
    }
    public AuthenticationResponse oundboundAuthenticate(String code) {
        var response = outBoundIdentityClient.exchangeToken(
                ExchangeTokenRequest.builder()
                        .code(code)
                        .clientId(CLIENT_ID)
                        .clientSecret(CLIENT_SECRET)
                        .redirectUri(REDIRECT_URI)
                        .grantType(GRANT_TYPE)
                .build());



        log.info("Token response: {}",response);
        Set<Role> roles=new HashSet<>();
        roles.add(Role.builder().name(PredefinedRole.USER_ROLE).build());
        //getuser info

        var userInfo=outboundUserClient.getUserInfo("json",response.getAccessToken());
        log.info("User Info: {}", userInfo);
        //Onboard
        var user=userRepository.findByUsername(userInfo.getEmail()).orElseGet(
                () -> userRepository.save(User.builder()
                        .username(userInfo.getEmail())
                        .firstName(userInfo.getGivenName())
                        .lastName(userInfo.getFamilyName())
                        .roles(roles)
                        .build()));
        //Generate token for user
        var token=generateToken(user);

        return  AuthenticationResponse.builder()
                .token(token)
                .build();

    }
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken =
                    InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException exception) {
            log.info("Token already expired");
        }
    }
    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signedJWT = verifyToken(request.getToken(), true);

        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

        invalidatedTokenRepository.save(invalidatedToken);

        var username = signedJWT.getJWTClaimsSet().getSubject();

        var user =
                userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .build();
    }

    private String generateToken(User user) {
        JWSHeader header=new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet jwtClaimsSet=new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("capoo.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION,ChronoUnit.SECONDS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .build();
        Payload payload=new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject=new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        }
        catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }


    private SignedJWT verifyToken(String token,boolean isRefresh) {
        try {
            JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);
            Date expiryTime = (isRefresh)
                    ? new Date(signedJWT
                    .getJWTClaimsSet()
                    .getIssueTime()
                    .toInstant()
                    .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                    .toEpochMilli())
                    : signedJWT.getJWTClaimsSet().getExpirationTime();            boolean verified = signedJWT.verify(verifier);
            if (!(verified && expiryTime.after(new Date())))
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            if (invalidatedTokenRepository.existsById(
                    signedJWT.getJWTClaimsSet().getJWTID()))
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            return signedJWT;
        } catch (JOSEException | ParseException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(user.getRoles()))
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions()))
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
            });

        return stringJoiner.toString();
    }
    @Transactional
    @Scheduled(fixedRate = 1800000) // mỗi 10 giây
    public void cleanupExpiredTokens() {
        log.info("Cleaning expired tokens...");
        invalidatedTokenRepository.deleteByExpiryTimeBefore(new Date());
    }

}
