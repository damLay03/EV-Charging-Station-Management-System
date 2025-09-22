package com.evstation.evchargingstation.repository;
import com.evstation.evchargingstation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // Tìm user theo username
    User findByUsername(String username);

    // Có thể thêm: tìm theo email nếu cần
    User findByEmail(String email);
}

