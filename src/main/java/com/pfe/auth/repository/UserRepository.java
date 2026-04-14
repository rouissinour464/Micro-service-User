package com.pfe.auth.repository;

import com.pfe.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Vérifie si un utilisateur existe par email
    boolean existsByEmail(String email);

    // Recherche un utilisateur par email
    Optional<User> findByEmail(String email);

    // Recherche par nom ou email (recherche insensible à la casse)
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> search(String keyword);
}