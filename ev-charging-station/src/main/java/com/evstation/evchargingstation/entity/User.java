package com.evstation.evchargingstation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "users") // ten bang trong Database
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE) // toàn bộ field là private
public class User {

//    @Id
//    @Column(name = "username", nullable = false, unique = true, length = 50)
//    String username;  // Primary Key
//
//    @Column(name = "password", nullable = false)
//    String password;
//
//    @Column(name = "email", nullable = false, unique = true)
//    String email;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String userId;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    String password;

    String phone;

    String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role;

    public enum Role { //Enum class, hỗ trợ cho class user lưu thằng role á
        ADMIN,
        DRIVER,
        STAFF
    }
}