package com.common.storage.config.datastore;

import org.springframework.context.annotation.Conditional;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Conditional(SqlDatastoreCondition.class)
@EnableJpaRepositories(basePackages = "com.common.storage.repository.jpa")
@EntityScan(basePackages = "com.common.storage.model.entity.jpa")
public class JpaPersistenceConfig {
}
