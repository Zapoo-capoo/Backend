package com.common.storage.config.datastore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Storage service uses PostgreSQL only. This post-processor pins the datastore
 * mode to "postgres" so the JPA repository adapter is always wired and the
 * service never silently falls back to an in-memory H2 database.
 */
public class DatastoreModeEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String DATASTORE_MODE_PROPERTY = "storage.datastore.mode";
    public static final String MODE_POSTGRES = "postgres";

    private static final String PROPERTY_SOURCE_NAME = "storageDatastoreMode";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(DATASTORE_MODE_PROPERTY, MODE_POSTGRES);
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
