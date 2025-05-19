package org.tanzu.mcpclient.vectorstore;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class DatabaseAvailableCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return DatabaseAvailabilityUtil.isDatabaseAvailable(context.getEnvironment());
    }
}