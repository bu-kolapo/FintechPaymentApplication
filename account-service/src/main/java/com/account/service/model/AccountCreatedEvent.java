package com.account.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountCreatedEvent implements Serializable {
    private String id;
    private String customerId;
    private String tenantId;

}
