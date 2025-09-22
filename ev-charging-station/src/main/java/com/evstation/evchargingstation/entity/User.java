package com.evstation.evchargingstation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Users") // ten bang trong Database
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;  // Primary Key

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", nullable = false, unique = true)
    private String email;
}