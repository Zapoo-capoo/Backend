package com.capoo.profile.configuration;

import com.capoo.profile.entity.UserProfile;
import com.capoo.profile.realsync.ElasticSyncIndexUserProfiles;
import com.capoo.profile.realsync.ElasticSyncUserProfileRepository;
import com.capoo.profile.repository.UserProfileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchInitializer {

    private final UserProfileRepository userProfileRepository;
    private final ElasticSyncUserProfileRepository elasticSyncUserProfileRepository;

    @PostConstruct
    public void initializeElasticsearchData() {
        log.info("Starting Elasticsearch data initialization...");

        try {
            // Get all user profiles from Neo4j
            List<UserProfile> profiles = userProfileRepository.findAll();

            if (profiles != null && !profiles.isEmpty()) {
                log.info("Found {} user profiles in Neo4j database", profiles.size());

                // Convert and index to Elasticsearch
                for (UserProfile profile : profiles) {
                    try {
                        ElasticSyncIndexUserProfiles elasticProfile = new ElasticSyncIndexUserProfiles(profile);
                        elasticSyncUserProfileRepository.save(elasticProfile);
                        log.debug("Indexed profile: {} ({})", profile.getUsername(), profile.getId());
                    } catch (Exception e) {
                        log.warn("Failed to index profile: {}", profile.getId(), e);
                    }
                }

                log.info("Elasticsearch data initialization completed successfully");
            } else {
                log.info("No user profiles found in Neo4j database");
            }

        } catch (Exception e) {
            log.error("Error during Elasticsearch initialization", e);
            // Don't throw exception to prevent app startup failure
        }
    }
}

