package com.helvino.ecommerce.repository;

import com.helvino.ecommerce.entity.User;
import com.helvino.ecommerce.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Page<User> findByRole(UserRole role, Pageable pageable);
    Page<User> findByRoleAndEnabledTrue(UserRole role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = 'CUSTOMER' AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<User> searchCustomers(String q, Pageable pageable);
}
