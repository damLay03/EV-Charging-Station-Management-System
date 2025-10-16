package com.swp.evchargingstation.entity;

import com.swp.evchargingstation.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "users") // ten bang trong Database
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE) // toàn bộ field là private
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String userId;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    String password;

    @Column(nullable = false)
    String phone;

    @Column(nullable = false)
    LocalDate dateOfBirth;

    @Column(nullable = false)
    boolean gender;

    @Column(nullable = false)
    String firstName;

    @Column(nullable = false)
    String lastName;

    @Transient
    String fullName;

    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role;
}
