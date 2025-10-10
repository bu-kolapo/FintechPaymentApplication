package com.commonlib.tenant;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;


public class TenantWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String tenantId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Tenant-Id");

        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        String finalTenantId = tenantId;
        return chain.filter(exchange)
                .contextWrite(Context.of(TenantContextHolder.TENANT_KEY, finalTenantId));
    }
}
