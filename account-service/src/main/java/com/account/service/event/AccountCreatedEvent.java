package com.account.service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountCreatedEvent implements Serializable {
    private String accountId;
    private String customerId;
    private String tenantId;

}
