package com.transaction.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public  class TransferRequest {
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    // getters/setters
}