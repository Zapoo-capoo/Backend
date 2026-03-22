package com.capoo.profile.repository;

import com.capoo.profile.entity.UserProfile;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends Neo4jRepository<UserProfile, String> {
    Optional<UserProfile> findByUserId(String userId);
    Optional<UserProfile> findByUsername(String username);
    List<UserProfile> findAllByUsernameLike(String username);

    @Query("""
    MATCH (u1:user_profile {id: $fromId})
    MATCH (u2:user_profile {id: $toId})
    WHERE u1.id <> u2.id
    
    // nếu đã là bạn thì bỏ
    OPTIONAL MATCH (u1)-[f:FRIEND]-(u2)
    
    // nếu đã có request từ u2 -> u1 thì accept luôn
    OPTIONAL MATCH (u2)-[r:FRIEND_REQUEST]->(u1)
    
    WITH u1, u2, f, r
    FOREACH (_ IN CASE WHEN r IS NOT NULL THEN [1] ELSE [] END |
        DELETE r
        MERGE (u1)-[:FRIEND]->(u2)
        MERGE (u2)-[:FRIEND]->(u1)
    )
    
    // nếu chưa có gì thì tạo request
    FOREACH (_ IN CASE WHEN r IS NULL AND f IS NULL THEN [1] ELSE [] END |
        MERGE (u1)-[:FRIEND_REQUEST]->(u2)
    )
    """)
    void sendFriendRequest(String fromId, String toId);

    @Query("""
    MATCH (u1:user_profile {id: $id1})
    MATCH (u2:user_profile {id: $id2})
    
    OPTIONAL MATCH (u1)-[f:FRIEND]-(u2)
    OPTIONAL MATCH (u1)-[r1:FRIEND_REQUEST]->(u2)
    OPTIONAL MATCH (u2)-[r2:FRIEND_REQUEST]->(u1)
    
    RETURN 
    CASE 
        WHEN f IS NOT NULL THEN 'FRIEND'
        WHEN r1 IS NOT NULL THEN 'SENT'
        WHEN r2 IS NOT NULL THEN 'RECEIVED'
        ELSE 'NONE'
    END AS status
    """)
    String getFriendStatus(String id1, String id2);
    @Query("""
    MATCH (u1:user_profile {id: $fromId})
    MATCH (u2:user_profile {id: $toId})
    
    OPTIONAL MATCH (u2)-[r:FRIEND_REQUEST]->(u1)
    DELETE r
    
    MERGE (u1)-[:FRIEND]->(u2)
    """)
    UserProfile acceptFriend(String fromId, String toId);

    @Query("""
    MATCH (u1:user_profile {id: $fromId})
    MATCH (u2:user_profile {id: $toId})
    
    OPTIONAL MATCH (u2)-[r:FRIEND_REQUEST]->(u1)
    DELETE r
    """)
    void rejectFriend(String fromId, String toId);

    @Query("""
    MATCH (u1:user_profile {id: $fromId})
    MATCH (u2:user_profile {id: $toId})
    
    OPTIONAL MATCH (u1)-[f:FRIEND]-(u2)
    DELETE f
    """)
    void unfriend(String fromId, String toId);

    @Query("""
    MATCH (u:user_profile {id: $id})-[:FRIEND_REQUEST]->(target)
    RETURN target
    """)
    List<UserProfile> getSentRequests(String id);

    @Query("""
    MATCH (u:user_profile {id: $id})<-[:FRIEND_REQUEST]-(sender)
    RETURN sender
    """)
    List<UserProfile> getReceivedRequests(String id);
    @Query("""
    MATCH (u:user_profile {id: $id})-[:FRIEND]-(f)
    RETURN f
    """)
    List<UserProfile> getFriend(String id);
}