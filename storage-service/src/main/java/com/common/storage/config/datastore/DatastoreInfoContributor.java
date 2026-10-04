package com.common.storage.config.datastore;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DatastoreInfoContributor implements InfoContributor {

    private final Environment environment;

    public DatastoreInfoContributor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("storage", Map.of(
                "datastoreMode",
                this.environment.getProperty(DatastoreModeEnvironmentPostProcessor.DATASTORE_MODE_PROPERTY, "h2")
        ));
    }
}
