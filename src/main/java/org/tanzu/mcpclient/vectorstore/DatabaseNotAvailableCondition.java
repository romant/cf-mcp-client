package org.tanzu.mcpclient.vectorstore;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

public class DatabaseNotAvailableCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
        return !DatabaseAvailabilityUtil.isDatabaseAvailable(context.getEnvironment());
    }
}