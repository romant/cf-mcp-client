package org.tanzu.mcpclient.web;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;
import org.tanzu.mcpclient.vectorstore.DatabaseAvailableCondition;

@Configuration
@EnableJdbcHttpSession(
        maxInactiveIntervalInSeconds = 86400 // 24 hours
)
@Order(2)
@Conditional(DatabaseAvailableCondition.class)
public class JdbcSessionConfiguration extends AbstractHttpSessionApplicationInitializer {
}