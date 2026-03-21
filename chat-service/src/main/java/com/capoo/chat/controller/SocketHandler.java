package com.capoo.chat.controller;

import com.capoo.chat.dto.request.IntrospectRequest;
import com.capoo.chat.dto.response.IntrospectResponse;
import com.capoo.chat.service.IdentityService;
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

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SocketHandler {
    SocketIOServer server;
    IdentityService identityService;
    @OnConnect
    public void clientConnected(SocketIOClient client) {

        //Get Token from request param
        String token = client.getHandshakeData().getSingleUrlParam("token");

        //Verify token and get user info
        IntrospectResponse introspectResponse=
                identityService.intrspectToken(
                        IntrospectRequest.builder()
                                .token(token)
                                .build()
                );
        //If token is valid, add user info to client session
        if (!introspectResponse.isValid()){
            log.error("introspectResponse is invalid");
            client.disconnect();
            return;
        }
        else{
            log.info("introspectResponse is valid");

        }
        log.info("Client connected:{}", client.getSessionId());
    }
    @OnDisconnect
    public void clientDisconnected(SocketIOClient client) {
        log.info("Client disconnected:{}", client.getSessionId());

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
