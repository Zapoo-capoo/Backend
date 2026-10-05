package com.capoo.gateway.configuration;

import com.capoo.gateway.dto.ApiResponse;
import com.capoo.gateway.service.IdentityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationFilter implements GlobalFilter, Ordered {
    IdentityService identityService;
    ObjectMapper objectMapper;
    @NonFinal
    String[] publicEndpoints=new String[]{
            "/identity/auth/.*","/identity/users/registration",
            "/notification/.*",
            "/file/media/download/.*",
            "/identity/v3/api-docs.*",
            "/profile/v3/api-docs.*",
            "/chat/v3/api-docs.*",
            "/post/v3/api-docs.*",
            "/storage-service/v3/api-docs.*",
            "/storage-service/files/(web|thumbnail|download|stream-video)/.*"

    };
    // Gateway's own swagger-ui assets (served without the api-prefix)
    @NonFinal
    String[] openEndpoints=new String[]{
            "/swagger-ui.html","/swagger-ui/.*","/v3/api-docs.*","/webjars/.*"
    };
    @Value("${app.api-prefix}")
    @NonFinal
    String apiPrefix;

    private static final String[] PUBLIC_ENDPOINTS = {
    };
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("AuthenticationFilter");
        //public endpoints
        if (isPublicEndpoint(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        // Get Token from header
        List<String> authHeader=exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (CollectionUtils.isEmpty(authHeader)) {
            return unAuthenticatedResponse(exchange.getResponse());
        }
        String token=authHeader.getFirst().substring("Bearer ".length());
        // Verify token: only the introspection result decides auth.
        // onErrorReturn(false) is placed BEFORE routing so a failing introspection
        // maps to 401, while errors from routing to a (possibly down) downstream
        // service are NOT masked as 401 — they propagate as their real status (e.g. 503).
        return identityService.introspectToken(token)
                .map(introspectResponseApiResponse ->
                        introspectResponseApiResponse.getResult().isValid())
                .onErrorReturn(false)
                .flatMap(valid -> valid
                        ? chain.filter(exchange)
                        : unAuthenticatedResponse(exchange.getResponse()));
    }
    @Override
    public int getOrder() {
        return -1;
    }
    private boolean isPublicEndpoint(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        boolean prefixed = Arrays.stream(publicEndpoints)
                .anyMatch(s -> path.matches(apiPrefix + s));
        boolean open = Arrays.stream(openEndpoints)
                .anyMatch(path::matches);
        return prefixed || open;
    }

    Mono<Void> unAuthenticatedResponse(ServerHttpResponse response) {
        ApiResponse<?> apiResponse=ApiResponse.builder()
                .code(HttpStatus.UNAUTHORIZED.value())
                .message("Unauthenticated")
                .build();
        String body;
        try {
            body=objectMapper.writeValueAsString(apiResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

}
