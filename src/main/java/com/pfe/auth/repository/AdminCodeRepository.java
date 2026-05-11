package com.pfe.auth.repository;

import com.pfe.auth.entity.AdminCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminCodeRepository extends JpaRepository<AdminCode, Long> {
    Optional<AdminCode> findByCode(String code);
}