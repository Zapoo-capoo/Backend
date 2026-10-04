package com.capoo.chat.service;

import org.springframework.stereotype.Service;

import com.capoo.chat.entity.WebSocketSession;
import com.capoo.chat.repository.WebSocketSessionRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WebSocketSessionServiceImpl implements WebSocketSessionService {
    WebSocketSessionRepository webSocketSessionRepository;

    @Override
    public WebSocketSession create(WebSocketSession webSocketSession) {
        return webSocketSessionRepository.save(webSocketSession);
    }

    @Override
    public void deleteSession(String sessionId) {
        webSocketSessionRepository.deleteBySocketSessionId(sessionId);
    }
}
