package org.tanzu.mcpclient.web;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.tanzu.mcpclient.document.VectorStoreConfiguration;

public class DatabaseAvailableCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return new VectorStoreConfiguration.VectorStoreCondition().matches(context, metadata);
    }
}