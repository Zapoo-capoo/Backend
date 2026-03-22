package com.capoo.profile.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Node("user_profile")
public class UserProfile {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    @Property("userId")
    String userId;
    String email;
    String username;
    String firstName;
    String city;
    LocalDate dob;
    String lastName;
    String avatar;
    @Relationship(type = "FRIEND", direction = Relationship.Direction.OUTGOING)
    private Set<UserProfile> friends;
    // request gửi đi
    @Relationship(type = "FRIEND_REQUEST", direction = Relationship.Direction.OUTGOING)
    private Set<UserProfile> sentRequests = new HashSet<>();
    // request nhận
    @Relationship(type = "FRIEND_REQUEST", direction = Relationship.Direction.INCOMING)
    private Set<UserProfile> receivedRequests = new HashSet<>();
}
