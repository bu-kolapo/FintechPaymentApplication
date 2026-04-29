package com.account.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerDTO {
    private String id;
    private String firstName;
    private String lastName;
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
