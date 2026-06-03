package com.capoo.profile.realsync;

import com.capoo.profile.entity.UserProfile;
import com.netflix.discovery.CommonConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.neo4j.core.schema.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "user_profiles")
public class ElasticSyncIndexUserProfiles {
    @Id
    private String id;
    private String userId;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String avatar;
    public ElasticSyncIndexUserProfiles(UserProfile userProfile) {
        this.id=userProfile.getId();
        this.userId=userProfile.getUserId();
        this.email=userProfile.getEmail();
        this.username=userProfile.getUsername();
        this.firstName=userProfile.getFirstName();
        this.lastName=userProfile.getLastName();
    }
}
