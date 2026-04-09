package com.customer.service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountCreatedEvent implements Serializable {
    private String id;
    private String accountId;
    private String customerId;
    private String tenantId;

}
