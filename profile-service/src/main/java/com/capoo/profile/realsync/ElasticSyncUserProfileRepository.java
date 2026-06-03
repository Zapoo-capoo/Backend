package com.capoo.profile.realsync;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElasticSyncUserProfileRepository extends
        ElasticsearchRepository<ElasticSyncIndexUserProfiles, String> {
    List<ElasticSyncIndexUserProfiles> findByUsernameContaining(String keyword);

}
