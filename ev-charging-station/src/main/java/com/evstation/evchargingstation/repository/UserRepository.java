package com.evstation.evchargingstation.repository;
import com.evstation.evchargingstation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    //Đây là cái nhà kho chứa users, vui lòng không xử lý
    boolean existsByEmail(String email);
    User findByEmail(String email);
}

