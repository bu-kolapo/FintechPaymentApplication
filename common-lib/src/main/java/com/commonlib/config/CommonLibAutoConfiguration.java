package com.commonlib.config;


import com.commonlib.tenant.TenantAwareMongoTemplate;
import com.commonlib.tenant.TenantWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;

@Configuration
public class CommonLibAutoConfiguration {

    @Bean
    public TenantWebFilter tenantWebFilter() {
        return new TenantWebFilter();
    }

    @Bean
    public TenantAwareMongoTemplate tenantAwareMongoTemplate(ReactiveMongoDatabaseFactory factory) {
        return new TenantAwareMongoTemplate(factory);
    }
}