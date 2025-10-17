package com.swp.evchargingstation.entity;

import com.swp.evchargingstation.enums.Gender;
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

    @Column(nullable = true)
    String phone;

    @Column(nullable = true)
    LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    Gender gender;

    @Column(nullable = true)
    String firstName;

    @Column(nullable = true)
    String lastName;

    @Transient
    String fullName;

    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    Role role;
}
