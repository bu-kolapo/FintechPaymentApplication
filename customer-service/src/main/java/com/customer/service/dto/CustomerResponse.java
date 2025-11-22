package com.customer.service.dto;


import com.customer.service.model.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponse {
    private String id;
    private String tenantId;

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String message;
    private LocalDate dateOfBirth;

    private Customer.CustomerStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    private List<String> accountIds; // optional reference to accounts

    public CustomerResponse(Object o, String message) {
    }


}
