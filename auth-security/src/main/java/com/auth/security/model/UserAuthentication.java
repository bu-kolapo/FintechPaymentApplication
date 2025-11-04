package com.auth.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
public class UserAuthentication {
    @Id
    private String id;   // or Long id, depending on your setup
    private String username;   // or Long id, depending on your setup
    private String password;
    private String role;

}
