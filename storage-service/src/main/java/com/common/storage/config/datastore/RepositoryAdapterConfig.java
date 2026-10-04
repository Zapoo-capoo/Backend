package com.common.storage.config.datastore;

import com.common.storage.repository.FileRepository;
import com.common.storage.repository.jpa.JpaFileRepository;
import com.common.storage.repository.jpa.adapter.JpaFileRepositoryAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryAdapterConfig {

    @Bean
    @Conditional(SqlDatastoreCondition.class)
    public FileRepository jpaFileRepositoryAdapter(JpaFileRepository jpaFileRepository) {
        return new JpaFileRepositoryAdapter(jpaFileRepository);
    }
}
