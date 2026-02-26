package com.kiwi.keweiaiagent.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "app.chat-memory", name = "type", havingValue = "mysql")
@ConditionalOnProperty(prefix = "app.datasource.mysql", name = "url")
@MapperScan(
        basePackages = "com.kiwi.keweiaiagent.chatmemory.mapper",
        sqlSessionFactoryRef = "chatMemorySqlSessionFactory"
)
    public class ChatMemoryMySqlDataSourceConfig {

    @Bean(name = "chatMemoryMySqlDataSourceProperties")
    @ConfigurationProperties(prefix = "app.datasource.mysql")
    public DataSourceProperties chatMemoryMySqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "chatMemoryMySqlDataSource")
    public DataSource chatMemoryMySqlDataSource(
            @Qualifier("chatMemoryMySqlDataSourceProperties") DataSourceProperties properties
    ) {
        // Use DataSourceProperties so `url` can be translated to Hikari's `jdbcUrl`.
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "mysqlChatMemoryJdbcTemplate")
    public JdbcTemplate mysqlChatMemoryJdbcTemplate(
            @Qualifier("chatMemoryMySqlDataSource") DataSource dataSource
    ) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "chatMemorySqlSessionFactory")
    public SqlSessionFactory chatMemorySqlSessionFactory(
            @Qualifier("chatMemoryMySqlDataSource") DataSource dataSource
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }

    @Bean(name = "chatMemorySqlSessionTemplate")
    public SqlSessionTemplate chatMemorySqlSessionTemplate(
            @Qualifier("chatMemorySqlSessionFactory") SqlSessionFactory sqlSessionFactory
    ) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
