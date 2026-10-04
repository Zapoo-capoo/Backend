package com.capoo.chat.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.capoo.chat.entity.WebSocketSession;

@Repository
public interface WebSocketSessionRepository extends MongoRepository<WebSocketSession, String> {

    void deleteBySocketSessionId(String socketSessionId);

    List<WebSocketSession> findAllByUserIdIn(List<String> userIds);
}
