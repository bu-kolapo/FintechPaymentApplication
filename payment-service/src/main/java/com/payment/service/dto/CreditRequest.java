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
public class CreditRequest {

    private String destinationAccount;
    private String accountId;
    private BigDecimal amount;
    private String referenceId;
    private String idempotencyKey;
    private String token;
}
