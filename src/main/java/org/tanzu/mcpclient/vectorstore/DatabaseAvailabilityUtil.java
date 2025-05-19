package org.tanzu.mcpclient.vectorstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseAvailabilityUtil {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseAvailabilityUtil.class);

    private static volatile Boolean databaseAvailable = null;

    /**
     * Checks if the database is available.
     * The result is cached after the first call.
     */
    public static boolean isDatabaseAvailable(Environment env) {
        if (databaseAvailable == null) {
            synchronized (DatabaseAvailabilityUtil.class) {
                if (databaseAvailable == null) {
                    databaseAvailable = testDirectJdbcConnection(env);
                    logger.info("Database availability check result: {}", databaseAvailable);
                }
            }
        }
        return databaseAvailable;
    }

    /**
     * Tests if a direct JDBC connection can be established.
     */
    private static boolean testDirectJdbcConnection(Environment env) {
        // Get JDBC properties from environment
        String url = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");
        String driverClassName = env.getProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");

        if (url == null) {
            logger.warn("No datasource URL found in environment properties");
            return false;
        }

        Connection connection = null;
        try {
            // Load the driver
            Class.forName(driverClassName);

            // Create direct connection
            connection = DriverManager.getConnection(url, username, password);
            boolean valid = connection.isValid(5); // 5 second timeout

            if (valid) {
                try (Statement stmt = connection.createStatement()) {
                    // Execute a simple query to verify DB is operational
                    stmt.execute("SELECT 1");
                }
            }

            logger.info("JDBC connection test: {}", valid ? "PASSED" : "FAILED");
            return valid;
        } catch (ClassNotFoundException e) {
            logger.error("JDBC driver not found: {} : {}", driverClassName, e.getMessage());
            return false;
        } catch (SQLException e) {
            logger.error("Direct JDBC connection failed: {}", e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.warn("Error closing JDBC connection: {}", e.getMessage());
                }
            }
        }
    }
}