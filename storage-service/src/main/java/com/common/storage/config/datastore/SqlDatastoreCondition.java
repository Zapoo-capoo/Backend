package com.common.storage.config.datastore;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class SqlDatastoreCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String mode = context.getEnvironment()
                .getProperty(DatastoreModeEnvironmentPostProcessor.DATASTORE_MODE_PROPERTY, "h2");
        return "h2".equalsIgnoreCase(mode) || "postgres".equalsIgnoreCase(mode);
    }
}
