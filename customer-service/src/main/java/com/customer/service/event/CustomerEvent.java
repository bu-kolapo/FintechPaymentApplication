package com.customer.service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerEvent implements Serializable {
    private String customerId;
    private String name;
    private String email;
    private String status;
}
