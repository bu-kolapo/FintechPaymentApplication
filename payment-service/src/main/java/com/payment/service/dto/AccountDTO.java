package com.payment.service.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountDTO {
    private String id;
    private String accountNumber;
    private String customerId;
    private String tenantId;
    private String currency;
    private BigDecimal openingBalance;
    private BigDecimal balance;
    private String status;
}
