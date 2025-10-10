package com.commonlib.tenant;


import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public class TenantContextHolder {

    public static final String TENANT_KEY = "TENANT_ID";

    public static <T> Mono<T> withTenant(Mono<T> mono, String tenantId) {
        return mono.contextWrite(Context.of(TENANT_KEY, tenantId));
    }

    public static Mono<String> getTenantId() {
        return Mono.deferContextual(ctx ->
                Mono.justOrEmpty(ctx.getOrDefault(TENANT_KEY, "default")));
    }
}
