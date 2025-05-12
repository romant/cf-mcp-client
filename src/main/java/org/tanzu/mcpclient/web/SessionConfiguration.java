package org.tanzu.mcpclient.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

@Configuration
@EnableJdbcHttpSession(
        maxInactiveIntervalInSeconds = 86400 // 24 hours
)
@Order(2)
public class SessionConfiguration extends AbstractHttpSessionApplicationInitializer {
}