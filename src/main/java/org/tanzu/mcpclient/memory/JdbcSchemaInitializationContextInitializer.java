package org.tanzu.mcpclient.memory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.tanzu.mcpclient.document.VectorStoreConfiguration;

@Order(Ordered.HIGHEST_PRECEDENCE)  // Ensures this runs as early as possible
public class JdbcSchemaInitializationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // Get access to environment and check database availability
        try {
            VectorStoreConfiguration.VectorStoreCondition condition = new VectorStoreConfiguration.VectorStoreCondition();
            boolean dbAvailable = condition.matches(new SpringConditionContextAdapter(applicationContext), null);

            // Set the property based on database availability
            System.setProperty("spring.ai.chat.memory.jdbc.initialize-schema", String.valueOf(dbAvailable));
            System.setProperty("spring.ai.chat.memory.repository.jdbc.initialize-schema", String.valueOf(dbAvailable));
        } catch (Exception e) {
            // If anything goes wrong, disable schema initialization to be safe
            System.setProperty("spring.ai.chat.memory.jdbc.initialize-schema", "false");
            System.setProperty("spring.ai.chat.memory.repository.jdbc.initialize-schema", "false");
        }
    }

    private class SpringConditionContextAdapter implements ConditionContext {
        private final ConfigurableApplicationContext context;

        public SpringConditionContextAdapter(ConfigurableApplicationContext context) {
            this.context = context;
        }

        @Override
        public ConfigurableListableBeanFactory getBeanFactory() {
            return context.getBeanFactory();
        }

        @Override
        public Environment getEnvironment() {
            return context.getEnvironment();
        }

        @Override
        public ClassLoader getClassLoader() {
            return context.getClassLoader();
        }

        @Override
        public ResourceLoader getResourceLoader() {
            return context;
        }

        @Override
        public BeanDefinitionRegistry getRegistry() {
            // The bean factory often implements BeanDefinitionRegistry
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            if (beanFactory instanceof BeanDefinitionRegistry) {
                return (BeanDefinitionRegistry) beanFactory;
            }
            // If it doesn't, you can return null or throw an exception
            // For most Spring contexts, this cast should succeed
            return null;
        }
    }
}