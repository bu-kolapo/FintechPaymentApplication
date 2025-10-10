package com.customer.service.services;

import com.customer.service.dto.CustomerRequest;
import com.customer.service.dto.CustomerResponse;
import com.customer.service.exception.CustomerCreationException;
import reactor.core.publisher.Mono;

public interface CustomerService {
    Mono<CustomerResponse> registerCustomer(CustomerRequest customerRequest) throws CustomerCreationException;
}
