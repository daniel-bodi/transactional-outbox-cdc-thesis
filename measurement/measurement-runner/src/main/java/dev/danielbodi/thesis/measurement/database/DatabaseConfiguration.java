package dev.danielbodi.thesis.measurement.database;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;

/**
 * @author danielbodi
 */
@Configuration
public class DatabaseConfiguration {

    @Bean
    @ConfigurationProperties("measurement.databases.subscription")
    DataSourceProperties subscriptionDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("measurement.databases.payment")
    DataSourceProperties paymentDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("measurement.databases.psp")
    DataSourceProperties pspDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    DataSource subscriptionDataSource(@Qualifier("subscriptionDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    DataSource paymentDataSource(@Qualifier("paymentDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    DataSource pspDataSource(@Qualifier("pspDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    JdbcClient subscriptionJdbcClient(@Qualifier("subscriptionDataSource") DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    JdbcClient paymentJdbcClient(@Qualifier("paymentDataSource") DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    JdbcClient pspJdbcClient(@Qualifier("pspDataSource") DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }
}
