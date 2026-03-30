package com.capoo.chat.controller;

import com.capoo.chat.dto.request.IntrospectRequest;
import com.capoo.chat.dto.response.IntrospectResponse;
import com.capoo.chat.entity.WebSocketSession;
import com.capoo.chat.service.IdentityService;
import com.capoo.chat.service.WebSocketSessionService;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class  SocketHandler {
    SocketIOServer server;
    IdentityService identityService;
    WebSocketSessionService webSocketSessionService;
    @OnConnect
    public void clientConnected(SocketIOClient client) {

        //Get Token from request param
        String token = client.getHandshakeData().getSingleUrlParam("token");

        //Verify token and get user info
        IntrospectResponse introspectResponse=
                identityService.introspectToken(
                        IntrospectRequest.builder()
                                .token(token)
                                .build()
                );

        //If token is valid, add user info to client session
        if (!introspectResponse.isValid()){
            log.error("introspectResponse is invalid");
            client.disconnect();

        }
        else{
            log.info("introspectResponse is valid");
            //Persist webSocetSession
            WebSocketSession webSocketSession=WebSocketSession.builder()
                    .socketSessionId(client.getSessionId().toString())
                    .userId(introspectResponse.getUserId())
                    .createdAt(Instant.now())
                    .build();
            webSocketSession= webSocketSessionService.create(webSocketSession);
            client.joinRoom("user-"+introspectResponse.getUserId());
            log.info("WebSocketSession created with userId:{}", webSocketSession.getUserId());
        }
    }
    @OnDisconnect
    public void clientDisconnected(SocketIOClient client) {
        log.info("Client disconnected:{}", client.getSessionId());
        webSocketSessionService.deleteSession(client.getSessionId().toString());

    }
    @PostConstruct
    public void startServer() {
        server.start();
        server.addListeners(this);
        log.info("Server started");
    }

    @PreDestroy
    public void stopServer() {
        server.stop();
        log.info("Socket server stopped");
    }

}
