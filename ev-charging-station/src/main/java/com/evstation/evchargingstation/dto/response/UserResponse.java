package com.evstation.evchargingstation.dto.response;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Entity
@Table(name = "users") // ten bang trong Database
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE) // toàn bộ field là private
public class UserResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String userId;

    @Column(nullable = false, unique = true)
    String email;

    //ngoai doi kh ai tra password ve
//    @Column(nullable = false)
//    String password;

    String phone;

    String fullName;

    Set<String> roles;


}