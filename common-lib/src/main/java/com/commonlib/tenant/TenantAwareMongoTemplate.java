package com.commonlib.tenant;

import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TenantAwareMongoTemplate extends ReactiveMongoTemplate {

    public TenantAwareMongoTemplate(ReactiveMongoDatabaseFactory factory) {
        super(factory);
    }

    @Override
    public <T> Flux<T> find(Query query, Class<T> entityClass) {
        return TenantContextHolder.getTenantId()
                .flatMapMany(tenantId -> {
                    query.addCriteria(Criteria.where("tenantId").is(tenantId));
                    return super.find(query, entityClass);
                });
    }

    @Override
    public <T> Mono<T> findOne(Query query, Class<T> entityClass) {
        return TenantContextHolder.getTenantId()
                .flatMap(tenantId -> {
                    query.addCriteria(Criteria.where("tenantId").is(tenantId));
                    return super.findOne(query, entityClass);
                });
    }
}