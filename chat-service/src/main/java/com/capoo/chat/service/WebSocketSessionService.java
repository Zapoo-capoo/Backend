package com.capoo.chat.service;

import com.capoo.chat.entity.WebSocketSession;

public interface WebSocketSessionService {
    WebSocketSession create(WebSocketSession webSocketSession);

    void deleteSession(String sessionId);
}
