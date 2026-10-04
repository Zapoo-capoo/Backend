package com.common.storage.config.datastore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatastoreModeLogger implements ApplicationRunner {

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        String mode = this.environment.getProperty(DatastoreModeEnvironmentPostProcessor.DATASTORE_MODE_PROPERTY, "h2");
        if (DatastoreModeEnvironmentPostProcessor.MODE_POSTGRES.equalsIgnoreCase(mode)) {
            log.info("Using PostgreSQL as primary datastore");
            return;
        }
        log.warn("No PostgreSQL config found. Falling back to H2 in-memory database.");
    }
}
