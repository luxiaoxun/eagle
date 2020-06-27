package com.alarm.eagle.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

/**
 * Created by luxiaoxun on 18/1/2.
 */
@Configuration
@EnableJpaRepositories("com.alarm.eagle.api.domain.repository")
public class DatasourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource getDataSouce() {
        return DataSourceBuilder.create().build();
    }
}
